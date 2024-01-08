package ru.hh.school;

import javax.sql.DataSource;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.BeforeClass;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.hh.school.util.QueryInfoHolder;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class BaseTest {

  protected static PostgreSQLContainer pgContainer;
  protected static DataSource dataSource;
  protected static SessionFactory sessionFactory;

  @BeforeClass
  public static void setupSessionFactory() throws IOException {
    pgContainer = new PostgreSQLContainer<>("postgres")
      .withUsername("postgres")
      .withPassword("postgres");
    pgContainer.start();

    PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
    String jdbcUrl = pgContainer.getJdbcUrl();
    pgDataSource.setUrl(jdbcUrl);
    pgDataSource.setUser(pgContainer.getUsername());
    pgDataSource.setPassword(pgContainer.getPassword());
    dataSource = pgDataSource;

    sessionFactory = DbFactory.createSessionFactory(dataSource);
  }

  @Before
  public void setUp() {
    QueryInfoHolder.clear();
  }

  protected Session getSession() {
    return sessionFactory.getCurrentSession();
  }

  protected <T> T doInTransaction(Supplier<T> supplier) {
    Optional<Transaction> transaction = beginTransaction();
    try {
      T result = supplier.get();
      transaction.ifPresent(Transaction::commit);
      return result;
    } catch (RuntimeException e) {
      transaction
        .filter(Transaction::isActive)
        .ifPresent(Transaction::rollback);
      throw e;
    }
  }

  protected void doInTransaction(Runnable runnable) {
    doInTransaction(() -> {
      runnable.run();
      return null;
    });
  }

  private Optional<Transaction> beginTransaction() {
    Transaction transaction = getSession().getTransaction();
    if (transaction.isActive()) {
      return Optional.empty();
    }
    transaction.begin();
    return Optional.of(transaction);
  }

  // методы ниже возвращают количество подготовленных JDBC стейтментов

  protected long getSelectCount() {
    return QueryInfoHolder.getQueryInfo().selectCount;
  }

  protected long getUpdateCount() {
    return QueryInfoHolder.getQueryInfo().updateCount;
  }

  protected long getInsertCount() {
    return QueryInfoHolder.getQueryInfo().insertCount;
  }

  protected long getDeleteCount() {
    return QueryInfoHolder.getQueryInfo().deleteCount;
  }

  protected long getCallCount() {
    return QueryInfoHolder.getQueryInfo().callCount;
  }

  protected long getTotalQueries() {
    return QueryInfoHolder.getQueryInfo().getTotalQueriesCount();
  }

  protected void clearQueriesCounts() {
    QueryInfoHolder.clear();
  }
}
