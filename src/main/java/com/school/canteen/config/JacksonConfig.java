package com.school.canteen.config;

import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 让 Jackson 正确序列化 Hibernate 懒加载代理（未初始化的关联输出为 null） */
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate5JakartaModule hibernate5JakartaModule() {
        Hibernate5JakartaModule module = new Hibernate5JakartaModule();
        // 序列化时强制加载懒加载关联，保证 API 输出完整（配合 open-in-view）
        module.configure(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING, true);
        return module;
    }
}
