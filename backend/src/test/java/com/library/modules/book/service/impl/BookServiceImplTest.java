package com.library.modules.book.service.impl;

import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.modules.author.entity.Author;
import com.library.modules.author.mapper.AuthorMapper;
import com.library.modules.book.dto.BookRequest;
import com.library.modules.book.dto.BookVO;
import com.library.modules.book.entity.Book;
import com.library.modules.book.mapper.BookAuthorMapper;
import com.library.modules.book.mapper.BookMapper;
import com.library.modules.category.mapper.CategoryMapper;
import com.library.modules.publisher.mapper.PublisherMapper;
import com.library.support.MpTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookMapper bookMapper;
    @Mock
    private BookAuthorMapper bookAuthorMapper;
    @Mock
    private AuthorMapper authorMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private PublisherMapper publisherMapper;

    private BookServiceImpl bookService;

    @BeforeEach
    void setUp() {
        MpTestSupport.initTableInfo(Book.class);
        bookService = new BookServiceImpl(bookMapper, bookAuthorMapper, authorMapper, categoryMapper, publisherMapper);
    }

    @Test
    void createNormalizesIsbnAndInitializesAvailableStock() {
        when(bookMapper.selectCount(any())).thenReturn(0L);
        when(authorMapper.selectBatchIds(any())).thenReturn(List.of(author(1L, "刘慈欣")));
        when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            book.setId(10L);
            return 1;
        });

        BookVO created = bookService.create(request("978-7-5366-9293-0", 6));

        assertThat(created.isbn()).isEqualTo("9787536692930");
        assertThat(created.totalStock()).isEqualTo(6);
        assertThat(created.availableStock()).isEqualTo(6);
        assertThat(created.status()).isEqualTo("AVAILABLE");
        verify(bookAuthorMapper).insertBatch(eq(10L), anyList());
    }

    @Test
    void createRejectsMalformedIsbn() {
        assertThatThrownBy(() -> bookService.create(request("978-ABC", 1)))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(ex.getMessage()).contains("ISBN");
                });
        verify(bookMapper, never()).insert(any(Book.class));
    }

    @Test
    void createRejectsDuplicateIsbn() {
        when(bookMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> bookService.create(request("9787536692930", 1)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE));
    }

    @Test
    void createRejectsUnknownAuthor() {
        when(bookMapper.selectCount(any())).thenReturn(0L);
        when(authorMapper.selectBatchIds(any())).thenReturn(List.of(author(1L, "刘慈欣")));

        assertThatThrownBy(() -> bookService.create(request("9787536692930", 1, 1L, 2L)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void updateStockKeepsBorrowedCopiesAvailable() {
        Book existing = book(1L, "9787536692930", 5, 2); // 已借出 3 本
        when(bookMapper.selectById(1L)).thenReturn(existing);

        BookVO updated = bookService.updateStock(1L, 8);

        assertThat(updated.totalStock()).isEqualTo(8);
        assertThat(updated.availableStock()).isEqualTo(5);
        assertThat(existing.getAvailableStock()).isEqualTo(5);
    }

    @Test
    void updateStockRejectsValueBelowBorrowedCount() {
        when(bookMapper.selectById(1L)).thenReturn(book(1L, "9787536692930", 5, 2));

        assertThatThrownBy(() -> bookService.updateStock(1L, 2))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(ex.getMessage()).contains("已借出");
                });
        verify(bookMapper, never()).updateById(any(Book.class));
    }

    @Test
    void deleteIsRejectedWhenBorrowRecordsExist() {
        when(bookMapper.selectById(1L)).thenReturn(book(1L, "9787536692930", 3, 3));
        when(bookMapper.countBorrowRecords(1L)).thenReturn(2L);

        assertThatThrownBy(() -> bookService.delete(1L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(ex.getMessage()).contains("下架");
                });
        verify(bookMapper, never()).deleteById(1L);
    }

    @Test
    void createWithAuthorNamesReusesExistingAndCreatesMissingAuthor() {
        when(bookMapper.selectCount(any())).thenReturn(0L);
        when(authorMapper.selectList(any())).thenReturn(List.of(author(1L, "刘慈欣")), List.of());
        when(authorMapper.insert(any(Author.class))).thenAnswer(invocation -> {
            Author author = invocation.getArgument(0);
            author.setId(9L);
            return 1;
        });
        when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            book.setId(10L);
            return 1;
        });
        when(bookAuthorMapper.selectByBookIds(anyList())).thenReturn(List.of(link(10L, 1L), link(10L, 9L)));
        when(authorMapper.selectBatchIds(any())).thenReturn(List.of(author(1L, "刘慈欣"), author(9L, "新作者")));

        BookVO created = bookService.create(requestByNames("9787536692930", 2, "刘慈欣", "新作者", " "));

        verify(authorMapper).insert(org.mockito.ArgumentMatchers.<Author>argThat(
                author -> "新作者".equals(author.getName())));
        org.mockito.ArgumentCaptor<List<Long>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(bookAuthorMapper).insertBatch(eq(10L), captor.capture());
        assertThat(captor.getValue()).containsExactly(1L, 9L);
        assertThat(created.authors()).extracting("name").containsExactly("刘慈欣", "新作者");
    }

    @Test
    void createWithPublisherNameCreatesPublisherWhenMissing() {
        when(bookMapper.selectCount(any())).thenReturn(0L);
        when(authorMapper.selectBatchIds(any())).thenReturn(List.of(author(1L, "刘慈欣")));
        when(publisherMapper.selectList(any())).thenReturn(List.of());
        when(publisherMapper.insert(any(com.library.modules.publisher.entity.Publisher.class)))
                .thenAnswer(invocation -> {
                    com.library.modules.publisher.entity.Publisher publisher = invocation.getArgument(0);
                    publisher.setId(5L);
                    return 1;
                });
        when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            book.setId(10L);
            return 1;
        });
        when(publisherMapper.selectBatchIds(any())).thenAnswer(invocation -> {
            com.library.modules.publisher.entity.Publisher publisher = new com.library.modules.publisher.entity.Publisher();
            publisher.setId(5L);
            publisher.setName("科幻出版社");
            return List.of(publisher);
        });

        BookVO created = bookService.create(requestWithPublisher("9787536692930", 1, "科幻出版社"));

        verify(publisherMapper).insert(org.mockito.ArgumentMatchers.<com.library.modules.publisher.entity.Publisher>argThat(
                publisher -> "科幻出版社".equals(publisher.getName())));
        assertThat(created.publisherId()).isEqualTo(5L);
        assertThat(created.publisherName()).isEqualTo("科幻出版社");
    }

    @Test
    void createWithoutAnyAuthorIsRejected() {
        when(bookMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> bookService.create(requestByNames("9787536692930", 1)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("至少填写一位作者");

        verify(bookMapper, never()).insert(any(Book.class));
    }

    @Test
    void deleteRemovesAuthorLinksBeforeBook() {
        when(bookMapper.selectById(1L)).thenReturn(book(1L, "9787536692930", 3, 3));
        when(bookMapper.countBorrowRecords(1L)).thenReturn(0L);

        bookService.delete(1L);

        verify(bookAuthorMapper).deleteByBookId(1L);
        verify(bookMapper).deleteById(1L);
    }

    // ---------------------------------------------------------------- 测试夹具

    private BookRequest request(String isbn, int totalStock) {
        return request(isbn, totalStock, 1L);
    }

    private BookRequest request(String isbn, int totalStock, Long... authorIds) {
        return new BookRequest(isbn, "三体", null, List.of(authorIds), null, null, null, null, null, "zh",
                new BigDecimal("23.00"), null, null, totalStock, null);
    }

    private BookRequest requestByNames(String isbn, int totalStock, String... authorNames) {
        return new BookRequest(isbn, "三体", null, null, List.of(authorNames), null, null, null, null, "zh",
                new BigDecimal("23.00"), null, null, totalStock, null);
    }

    private BookRequest requestWithPublisher(String isbn, int totalStock, String publisherName) {
        return new BookRequest(isbn, "三体", null, List.of(1L), null, null, publisherName, null, null, "zh",
                new BigDecimal("23.00"), null, null, totalStock, null);
    }

    private com.library.modules.book.entity.BookAuthor link(Long bookId, Long authorId) {
        com.library.modules.book.entity.BookAuthor link = new com.library.modules.book.entity.BookAuthor();
        link.setBookId(bookId);
        link.setAuthorId(authorId);
        return link;
    }

    private Book book(Long id, String isbn, int totalStock, int availableStock) {
        Book book = new Book();
        book.setId(id);
        book.setIsbn(isbn);
        book.setTitle("三体");
        book.setTotalStock(totalStock);
        book.setAvailableStock(availableStock);
        book.setStatus("AVAILABLE");
        return book;
    }

    private Author author(Long id, String name) {
        Author author = new Author();
        author.setId(id);
        author.setName(name);
        return author;
    }
}
