package se.ifmo.ru.lab1.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Set;

@ApplicationPath("/api")
public class JaxRsApplication extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
            SpaceMarineResource.class,
            ChapterResource.class,
            CoordinatesResource.class,
            SpecialOperationsResource.class
        );
    }
}
