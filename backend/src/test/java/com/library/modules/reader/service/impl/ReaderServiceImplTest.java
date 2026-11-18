package com.library.modules.reader.service.impl;

import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.config.BorrowProperties;
import com.library.modules.reader.dto.ReaderRequest;
import com.library.modules.reader.dto.ReaderVO;
import com.library.modules.reader.entity.Reader;
import com.library.modules.reader.mapper.ReaderBorrowCount;
import com.library.modules.reader.mapper.ReaderMapper;
import com.library.modules.user.entity.User;
import com.library.modules.user.mapper.UserMapper;
import com.library.security.AuthUser;
import com.library.support.MpTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReaderServiceImplTest {

    private static final Long READER_USER_ID = 3L;

    @Mock
    private ReaderMapper readerMapper;
    @Mock
    private UserMapper userMapper;

    private ReaderServiceImpl readerService;

    @BeforeEach
    void setUp() {
        MpTestSupport.initTableInfo(Reader.class);
        BorrowProperties properties = new BorrowProperties(30, 2, 15, 5,
                new BigDecimal("0.50"), false, new BigDecimal("100.00"));
        readerService = new ReaderServiceImpl(readerMapper, userMapper, properties);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createFallsBackToGlobalMaxBorrowAndActiveStatus() {
        when(readerMapper.selectCount(any())).thenReturn(0L);
        when(readerMapper.insert(any(Reader.class))).thenAnswer(invocation -> {
            Reader reader = invocation.getArgument(0);
            reader.setId(7L);
            return 1;
        });

        ReaderVO created = readerService.create(new ReaderRequest("R2024009", " 李四 ",
                null, null, null, null, null));

        assertThat(created.name()).isEqualTo("李四");
        assertThat(created.maxBorrow()).isEqualTo(5);
        assertThat(created.status()).isEqualTo("ACTIVE");
        assertThat(created.userId()).isNull();
    }

    @Test
    void createRejectsDuplicateCardNo() {
        when(readerMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> readerService.create(new ReaderRequest("R2024001", "张三",
                null, null, null, null, null)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE));
        verify(readerMapper, never()).insert(any(Reader.class));
    }

    @Test
    void createRejectsUserAlreadyBoundToAnotherReader() {
        when(readerMapper.selectCount(any())).thenReturn(0L, 1L);
        when(userMapper.selectById(9L)).thenReturn(user(9L));

        assertThatThrownBy(() -> readerService.create(new ReaderRequest("R2024010", "王五",
                null, null, null, null, 9L)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE));
    }

    @Test
    void createRejectsUnknownUserBinding() {
        when(readerMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> readerService.create(new ReaderRequest("R2024011", "赵六",
                null, null, null, null, 99L)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void deleteIsRejectedWhenBorrowRecordsExist() {
        when(readerMapper.selectById(1L)).thenReturn(reader(1L, 3L));
        when(readerMapper.countBorrowRecords(1L)).thenReturn(2L);

        assertThatThrownBy(() -> readerService.delete(1L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(ex.getMessage()).contains("停用");
                });
        verify(readerMapper, never()).deleteById(1L);
    }

    @Test
    void getMineReturnsProfileOfCurrentUserWithBorrowingCount() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthUser(READER_USER_ID, "reader01", List.of("READER")), null, List.of()));
        when(readerMapper.selectOne(any())).thenReturn(reader(1L, READER_USER_ID));
        when(readerMapper.countBorrowingByReaderIds(List.of(1L))).thenReturn(List.of(count(1L, 2L)));

        ReaderVO mine = readerService.getMine();

        assertThat(mine.id()).isEqualTo(1L);
        assertThat(mine.borrowingCount()).isEqualTo(2L);
    }

    @Test
    void getMineReportsMissingProfile() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthUser(READER_USER_ID, "reader01", List.of("READER")), null, List.of()));
        when(readerMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> readerService.getMine())
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    assertThat(ex.getMessage()).contains("未绑定");
                });
    }

    // ---------------------------------------------------------------- 测试夹具

    private Reader reader(Long id, Long userId) {
        Reader reader = new Reader();
        reader.setId(id);
        reader.setUserId(userId);
        reader.setCardNo("R2024001");
        reader.setName("张三");
        reader.setStatus("ACTIVE");
        reader.setMaxBorrow(5);
        return reader;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("reader02");
        return user;
    }

    private ReaderBorrowCount count(Long readerId, Long borrowCount) {
        ReaderBorrowCount count = new ReaderBorrowCount();
        count.setReaderId(readerId);
        count.setBorrowCount(borrowCount);
        return count;
    }
}
