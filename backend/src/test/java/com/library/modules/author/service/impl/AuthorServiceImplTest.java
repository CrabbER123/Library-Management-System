package com.library.modules.author.service.impl;

import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.modules.author.dto.AuthorRequest;
import com.library.modules.author.dto.AuthorVO;
import com.library.modules.author.entity.Author;
import com.library.modules.author.mapper.AuthorMapper;
import com.library.modules.book.mapper.BookAuthorMapper;
import com.library.support.MpTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorServiceImplTest {

    @Mock
    private AuthorMapper authorMapper;
    @Mock
    private BookAuthorMapper bookAuthorMapper;

    private AuthorServiceImpl authorService;

    @BeforeEach
    void setUp() {
        MpTestSupport.initTableInfo(Author.class);
        authorService = new AuthorServiceImpl(authorMapper, bookAuthorMapper);
    }

    @Test
    void createTrimsInputAndClearsBlankFields() {
        when(authorMapper.selectCount(any())).thenReturn(0L);
        when(authorMapper.insert(any(Author.class))).thenAnswer(invocation -> {
            Author author = invocation.getArgument(0);
            author.setId(11L);
            return 1;
        });

        AuthorVO created = authorService.create(new AuthorRequest("  刘慈欣 ", "   ", " 科幻作家 "));

        assertThat(created.name()).isEqualTo("刘慈欣");
        assertThat(created.nationality()).isNull();
        assertThat(created.intro()).isEqualTo("科幻作家");
    }

    @Test
    void createRejectsDuplicateName() {
        when(authorMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> authorService.create(new AuthorRequest("刘慈欣", "中国", null)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE));
        verify(authorMapper, never()).insert(any(Author.class));
    }

    @Test
    void deleteIsRejectedWhenAuthorStillLinkedToBooks() {
        when(authorMapper.selectById(1L)).thenReturn(author(1L, "刘慈欣"));
        when(bookAuthorMapper.countByAuthorId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> authorService.delete(1L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(ex.getMessage()).contains("3");
                });
        verify(authorMapper, never()).deleteById(1L);
    }

    @Test
    void deleteRemovesUnlinkedAuthor() {
        when(authorMapper.selectById(1L)).thenReturn(author(1L, "刘慈欣"));
        when(bookAuthorMapper.countByAuthorId(1L)).thenReturn(0L);

        authorService.delete(1L);

        verify(authorMapper).deleteById(1L);
    }

    @Test
    void deleteReportsMissingAuthor() {
        when(authorMapper.selectById(42L)).thenReturn(null);

        assertThatThrownBy(() -> authorService.delete(42L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    private Author author(Long id, String name) {
        Author author = new Author();
        author.setId(id);
        author.setName(name);
        return author;
    }
}
