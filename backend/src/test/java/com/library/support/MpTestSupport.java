package com.library.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 纯 Mockito 单测里没有 Spring 容器，MyBatis-Plus 的 lambda 列名缓存不会被自动构建，
 * 调用 {@code lambdaUpdate().set(...)} 这类会立即解析列名的方法时会抛
 * "can not find lambda cache for this entity"。这里手动初始化一次。
 */
public final class MpTestSupport {

    private static final Set<Class<?>> INITIALIZED = new HashSet<>();

    private MpTestSupport() {
    }

    public static synchronized void initTableInfo(Class<?>... entities) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.library.test");
        Arrays.stream(entities)
                .filter(entity -> !INITIALIZED.contains(entity))
                .forEach(entity -> {
                    TableInfoHelper.initTableInfo(assistant, entity);
                    INITIALIZED.add(entity);
                });
    }
}
