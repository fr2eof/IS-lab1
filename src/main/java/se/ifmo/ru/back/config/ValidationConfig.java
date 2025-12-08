package se.ifmo.ru.back.config;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

@Configuration
public class ValidationConfig {

    /**
     * Настройка Validator для использования Spring Bean Factory
     * Это позволяет валидаторам использовать @Autowired
     */
    @Bean
    @Primary
    public LocalValidatorFactoryBean validator(AutowireCapableBeanFactory beanFactory) {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setConstraintValidatorFactory(new SpringConstraintValidatorFactory(beanFactory));
        return factory;
    }
}

