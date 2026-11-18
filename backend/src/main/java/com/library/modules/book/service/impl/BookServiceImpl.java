package com.library.modules.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.common.query.Sorts;
import com.library.common.result.PageResult;
import com.library.modules.author.entity.Author;
import com.library.modules.author.mapper.AuthorMapper;
import com.library.modules.book.dto.AuthorBrief;
import com.library.modules.book.dto.BookRequest;
import com.library.modules.book.dto.BookVO;
import com.library.modules.book.entity.Book;
import com.library.modules.book.entity.BookAuthor;
import com.library.modules.book.mapper.BookAuthorMapper;
import com.library.modules.book.mapper.BookMapper;
import com.library.modules.book.service.BookService;
import com.library.modules.category.entity.Category;
import com.library.modules.category.mapper.CategoryMapper;
import com.library.modules.publisher.entity.Publisher;
import com.library.modules.publisher.mapper.PublisherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private static final Set<String> STATUSES = Set.of("AVAILABLE", "OFFLINE");
    private static final String DEFAULT_STATUS = "AVAILABLE";
    private static final String ISBN_PATTERN = "^(\\d{9}[\\dX]|\\d{13})$";

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "id",
            "title", "title",
            "createdAt", "created_at",
            "publishDate", "publish_date",
            "price", "price",
            "totalStock", "total_stock",
            "availableStock", "available_stock");

    private final BookMapper bookMapper;
    private final BookAuthorMapper bookAuthorMapper;
    private final AuthorMapper authorMapper;
    private final CategoryMapper categoryMapper;
    private final PublisherMapper publisherMapper;

    @Override
    public PageResult<BookVO> list(int page, int size, String keyword, Long categoryId, Long authorId,
                                   Long publisherId, String status, String sort) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);

        QueryWrapper<Book> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            String like = "%" + kw + "%";
            wrapper.and(w -> w.like("title", kw)
                    .or().like("subtitle", kw)
                    .or().like("isbn", kw)
                    .or().apply("id IN (SELECT ba.book_id FROM book_authors ba "
                            + "JOIN authors a ON a.id = ba.author_id WHERE a.name LIKE {0})", like));
        }
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        if (publisherId != null) {
            wrapper.eq("publisher_id", publisherId);
        }
        if (authorId != null) {
            wrapper.apply("id IN (SELECT book_id FROM book_authors WHERE author_id = {0})", authorId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status.trim());
        }
        if (Sorts.apply(wrapper, sort, SORT_COLUMNS) == null) {
            Sorts.applyDefault(wrapper, "id", false);
        }

        Page<Book> result = bookMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<BookVO> records = toVOs(result.getRecords());
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize(),
                result.getPages());
    }

    @Override
    public BookVO getById(Long id) {
        return toVOs(List.of(requireBook(id))).get(0);
    }

    @Override
    @Transactional
    public BookVO create(BookRequest request) {
        String isbn = normalizeIsbn(request.isbn());
        ensureIsbnAvailable(isbn, null);

        Set<Long> authorIds = validateAuthors(request.authorIds());
        validateCategory(request.categoryId());
        validatePublisher(request.publisherId());
        String status = normalizeStatus(request.status());

        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(request.title().trim());
        book.setSubtitle(trimToNull(request.subtitle()));
        book.setPublisherId(request.publisherId());
        book.setCategoryId(request.categoryId());
        book.setPublishDate(request.publishDate());
        book.setLanguage(trimToNull(request.language()));
        book.setPrice(request.price());
        book.setCoverUrl(trimToNull(request.coverUrl()));
        book.setSummary(trimToNull(request.summary()));
        book.setTotalStock(request.totalStock());
        book.setAvailableStock(request.totalStock());
        book.setStatus(status);
        bookMapper.insert(book);

        bookAuthorMapper.insertBatch(book.getId(), new ArrayList<>(authorIds));
        return toVOs(List.of(book)).get(0);
    }

    @Override
    @Transactional
    public BookVO update(Long id, BookRequest request) {
        Book book = requireBook(id);
        String isbn = normalizeIsbn(request.isbn());
        if (!isbn.equals(book.getIsbn())) {
            ensureIsbnAvailable(isbn, id);
        }
        Set<Long> authorIds = validateAuthors(request.authorIds());
        validateCategory(request.categoryId());
        validatePublisher(request.publisherId());

        int borrowed = book.getTotalStock() - book.getAvailableStock();
        if (request.totalStock() < borrowed) {
            throw new BizException(ErrorCode.VALIDATION_ERROR,
                    "总库存不能小于已借出数量（当前借出 " + borrowed + " 本）");
        }

        book.setIsbn(isbn);
        book.setTitle(request.title().trim());
        book.setSubtitle(trimToNull(request.subtitle()));
        book.setPublisherId(request.publisherId());
        book.setCategoryId(request.categoryId());
        book.setPublishDate(request.publishDate());
        book.setLanguage(trimToNull(request.language()));
        book.setPrice(request.price());
        book.setCoverUrl(trimToNull(request.coverUrl()));
        book.setSummary(trimToNull(request.summary()));
        book.setTotalStock(request.totalStock());
        book.setAvailableStock(request.totalStock() - borrowed);
        book.setStatus(normalizeStatus(request.status()));
        bookMapper.updateById(book);

        bookAuthorMapper.deleteByBookId(id);
        bookAuthorMapper.insertBatch(id, new ArrayList<>(authorIds));
        return toVOs(List.of(book)).get(0);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        if (!STATUSES.contains(status)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不支持的图书状态: " + status);
        }
        Book book = requireBook(id);
        book.setStatus(status);
        bookMapper.updateById(book);
    }

    @Override
    @Transactional
    public BookVO updateStock(Long id, int totalStock) {
        Book book = requireBook(id);
        int borrowed = book.getTotalStock() - book.getAvailableStock();
        if (totalStock < borrowed) {
            throw new BizException(ErrorCode.VALIDATION_ERROR,
                    "总库存不能小于已借出数量（当前借出 " + borrowed + " 本）");
        }
        book.setTotalStock(totalStock);
        book.setAvailableStock(totalStock - borrowed);
        bookMapper.updateById(book);
        return toVOs(List.of(book)).get(0);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireBook(id);
        long records = bookMapper.countBorrowRecords(id);
        if (records > 0) {
            throw new BizException(ErrorCode.CONFLICT,
                    "该图书已有 " + records + " 条借阅记录，不能删除；可将其状态改为下架");
        }
        bookAuthorMapper.deleteByBookId(id);
        bookMapper.deleteById(id);
    }

    // ---------------------------------------------------------------- 内部方法

    /**
     * 批量装配分类/出版社名称与作者列表，避免逐条查询造成 N+1。
     */
    private List<BookVO> toVOs(List<Book> books) {
        if (books.isEmpty()) {
            return List.of();
        }
        List<Long> bookIds = books.stream().map(Book::getId).toList();

        Map<Long, List<Long>> authorIdsByBook = new HashMap<>();
        for (BookAuthor link : bookAuthorMapper.selectByBookIds(bookIds)) {
            authorIdsByBook.computeIfAbsent(link.getBookId(), key -> new ArrayList<>()).add(link.getAuthorId());
        }

        Set<Long> allAuthorIds = new HashSet<>();
        authorIdsByBook.values().forEach(allAuthorIds::addAll);
        Map<Long, String> authorNames = allAuthorIds.isEmpty()
                ? new HashMap<>()
                : authorMapper.selectBatchIds(allAuthorIds).stream()
                .collect(Collectors.toMap(Author::getId, Author::getName));

        Map<Long, String> categoryNames = names(books.stream().map(Book::getCategoryId), categoryMapper::selectBatchIds,
                Category::getId, Category::getName);
        Map<Long, String> publisherNames = names(books.stream().map(Book::getPublisherId),
                publisherMapper::selectBatchIds, Publisher::getId, Publisher::getName);

        return books.stream().map(book -> {
            List<AuthorBrief> authors = authorIdsByBook.getOrDefault(book.getId(), List.of()).stream()
                    .filter(authorNames::containsKey)
                    .map(authorId -> new AuthorBrief(authorId, authorNames.get(authorId)))
                    .toList();
            return new BookVO(book.getId(), book.getIsbn(), book.getTitle(), book.getSubtitle(), authors,
                    book.getPublisherId(), publisherNames.get(book.getPublisherId()),
                    book.getCategoryId(), categoryNames.get(book.getCategoryId()),
                    book.getPublishDate(), book.getLanguage(), book.getPrice(), book.getCoverUrl(),
                    book.getSummary(), book.getTotalStock(), book.getAvailableStock(), book.getStatus(),
                    book.getCreatedAt(), book.getUpdatedAt());
        }).toList();
    }

    private <T> Map<Long, String> names(java.util.stream.Stream<Long> idStream,
                                        Function<Set<Long>, List<T>> loader,
                                        Function<T, Long> idGetter, Function<T, String> nameGetter) {
        Set<Long> ids = idStream.filter(java.util.Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        if (ids.isEmpty()) {
            // 用可变 Map 而非 Map.of()：调用方会以可能为 null 的 categoryId/publisherId 查询，
            // 不可变 Map（MapN）对 null key 的 get 会抛 NPE。
            return new HashMap<>();
        }
        Map<Long, String> map = new LinkedHashMap<>();
        for (T item : loader.apply(ids)) {
            map.put(idGetter.apply(item), nameGetter.apply(item));
        }
        return map;
    }

    private String normalizeIsbn(String raw) {
        String isbn = raw.replace("-", "").replace(" ", "").toUpperCase();
        if (!isbn.matches(ISBN_PATTERN)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "ISBN 格式不正确，应为 10 位或 13 位数字");
        }
        return isbn;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return DEFAULT_STATUS;
        }
        String value = status.trim().toUpperCase();
        if (!STATUSES.contains(value)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "不支持的图书状态: " + status);
        }
        return value;
    }

    private void ensureIsbnAvailable(String isbn, Long excludeId) {
        Long count = bookMapper.selectCount(Wrappers.<Book>lambdaQuery()
                .eq(Book::getIsbn, isbn)
                .ne(excludeId != null, Book::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "ISBN 已存在: " + isbn);
        }
    }

    private Set<Long> validateAuthors(List<Long> authorIds) {
        Set<Long> ids = authorIds.stream().filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (ids.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "至少选择一位作者");
        }
        if (authorMapper.selectBatchIds(ids).size() != ids.size()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "存在无效的作者，请刷新后重试");
        }
        return ids;
    }

    private void validateCategory(Long categoryId) {
        if (categoryId != null && categoryMapper.selectById(categoryId) == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "分类不存在: " + categoryId);
        }
    }

    private void validatePublisher(Long publisherId) {
        if (publisherId != null && publisherMapper.selectById(publisherId) == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "出版社不存在: " + publisherId);
        }
    }

    private Book requireBook(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "图书不存在: " + id);
        }
        return book;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
