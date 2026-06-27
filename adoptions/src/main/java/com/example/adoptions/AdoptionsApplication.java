package com.example.adoptions;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.registry.ImportHttpServices;

import java.lang.annotation.*;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class AdoptionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdoptionsApplication.class, args);
    }

    @Bean
    JdbcPostgresDialect jdbcPostgresDialect() {
        return JdbcPostgresDialect.INSTANCE;
    }

    @Bean
    ApplicationRunner runner(DogRepository repository) {
        return _ -> repository.findAll().forEach(IO::println);
    }

    private static void test(DogRepository repository) {
        repository.findAll().forEach(IO::println);
    }
}

// GraalVM || Project Leyden
interface DogRepository extends ListCrudRepository<Dog, Integer> {

    // Ahead of Time (AOT)
    Collection<Dog> findByOwner(String owner);
}

record Dog(@Id int id, String owner, String name, String description) {
}

record CatFact(String fact) {
}

record CatFacts(Collection<CatFact> facts) {
}


@Configuration
@EnableResilientMethods
@ImportHttpServices(CatFactsClient.class)
class CatFactsConfiguration {
}

interface CatFactsClient {

    @GetExchange("https://www.catfacts.net/api")
    CatFacts facts();
}


/*
@Component
class CatFactsClient {

    private final RestClient http;

    CatFactsClient(RestClient.Builder http) {
        this.http = http.build();
    }

    CatFacts facts() {
        return this.http
                .get()
                .uri("https://www.catfacts.net/api")
                .retrieve()
                .body(CatFacts.class);
    }
}
*/

class MyBeanRegistrar implements BeanRegistrar {

    @Override
    public void register(BeanRegistry registry, Environment env) {
        for (var i = 0; i < 10; i++)
            registry.registerBean(MyRunner.class, a -> a.supplier(
                    _ -> new MyRunner()));
    }
}

@Configuration
//@Import(MyBeanRegistrar.class)
class MyConfig {

    @Bean
    MyRunner runner1() {
        return new MyRunner();
    }
}

// stereotype UML (unified modeling language)
class MyRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        IO.println("Hello, World!!!");
    }
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@interface NaijaComponent {

    /**
     * Alias for {@link Component#value}.
     */
    @AliasFor(annotation = Component.class)
    String value() default "";

}

@Controller
@ResponseBody
class CatsFactsController {

    private final CatFactsClient client;

    private final AtomicInteger counter = new AtomicInteger(0);

    CatsFactsController(CatFactsClient client) {
        this.client = client;
    }

    @ConcurrencyLimit(10)
    @Retryable(maxRetries = 5, includes = IllegalStateException.class)
    @GetMapping("/cats")
    Collection<CatFact> facts() {
        if (this.counter.getAndIncrement() < 5) {
            IO.println("no results!");
            throw new IllegalStateException("no results");
        }
        IO.println("got results");

        return this.client.facts().facts();
    }
}


@Controller
@ResponseBody
class DogsController {

    private final DogRepository repository;

    DogsController(DogRepository repository) {
        this.repository = repository;
    }

    @GetMapping(value = "/dogs", version = "2.0")
    Collection<Dog> dogsv2() {
        return repository.findAll();
    }

    @GetMapping(value = "/dogs", version = "1.0")
    Collection<Map<String, Object>> dogsv1() {
        return repository
                .findAll()
                .stream()
                .map(dog -> Map.of("id", (Object) dog.id(),
                        "fullName", dog.name()))
                .toList();
    }
}

// SPRING FRAMEWORK
// - dependency injection
// - aspect oriented programming
// - portable service abstractions

// SPRING BOOT
// - autoconfiguration

/*
@Repository
@Transactional
class DogRepository {

    private final JdbcClient db;

    DogRepository(JdbcClient db) {
        this.db = db;
    }

    public Collection<Dog> findAll() {
        return db
                .sql("select * from dog ")
                .query((res, _) -> new Dog(res.getInt("id"),
                        res.getString("owner"),
                        res.getString("name"),
                        res.getString("description")))
                .list();
    }
}
*/

//interface Tx {
//}


// jpa, jooq, jdo, kafka, hibernate, dbi, amqp, neo4j, mongodb,
// elasticsearch, jms, jetbrains exposed, ...


// start some work
//  - step 1
//  - step 2
//  - step 3
// commit || rollback

/*


//
//    @Bean
//    DogRepository dogRepository(JdbcClient jdbcClient) {
//        return new DogRepository(jdbcClient);
//    }

class TransactionalDogRepository implements DogRepository {

    private final DogRepository delegate;
    private final TransactionTemplate template;

    TransactionalDogRepository(DogRepository delegate, TransactionTemplate template) {
        this.template = template;
        this.delegate = delegate;
    }

    @Override
    public Collection<Dog> findAll() {
        return this.template.execute(_ -> this.delegate.findAll());
    }
}
*/
//
//interface DogRepository {
//    Collection<Dog> findAll();
//}


/*


    @SuppressWarnings("unchecked")
    private static Object transactional(Object target, Class<?> intface,
                                        TransactionTemplate template) {
        var pfb = new ProxyFactoryBean();
        pfb.setTargetClass(intface);
        pfb.setTarget(target);
        pfb.setProxyTargetClass(true);
        pfb.addAdvice((MethodInterceptor) invocation ->
                doInvoke(template, target, invocation.getMethod(), invocation.getArguments()));
        return pfb.getObject();
    }

    private static Object doInvoke(TransactionTemplate template, Object target, Method method, Object[] args) {
        try {
            IO.println("Executing " + method.getName());
            return template.execute(_ -> {
                try {
                    return method.invoke(target, args);
                }//
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    static TxBeanPostProcessor txBeanPostProcessor(ApplicationContext applicationContext) {
        return new TxBeanPostProcessor(applicationContext);
    }
   static class TxBeanPostProcessor implements BeanPostProcessor {

        private final ApplicationContext applicationContext;

        TxBeanPostProcessor(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        public @Nullable Object postProcessAfterInitialization(
                Object bean, String beanName) throws BeansException {
            IO.println("postProcessAfterInitialization " + beanName);
            if (bean instanceof Tx) {
                IO.println("we found a Transactional bean: " + beanName);
                return transactional(bean, bean.getClass(), applicationContext
                        .getBean(TransactionTemplate.class));
            }
            return bean;
        }
    }
*/