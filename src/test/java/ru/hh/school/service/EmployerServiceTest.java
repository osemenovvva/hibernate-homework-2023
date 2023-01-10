package ru.hh.school.service;

import org.hibernate.LazyInitializationException;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.hh.school.BaseTest;
import ru.hh.school.TestHelper;
import ru.hh.school.dao.EmployerDao;
import ru.hh.school.dao.GenericDao;
import ru.hh.school.entity.Employer;
import ru.hh.school.entity.Vacancy;
import ru.hh.school.util.TransactionHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EmployerServiceTest extends BaseTest {
  private static EmployerService employerService;
  private static GenericDao genericDao;

  @BeforeClass
  public static void setupService() {
    genericDao = new GenericDao(sessionFactory);

    employerService = new EmployerService(
        genericDao,
        new EmployerDao(sessionFactory),
        new TransactionHelper(sessionFactory)
    );
    TestHelper.executeScript(dataSource, "create_employers.sql");
  }

  @Test
  public void newEmployerSavingTest() {
    Employer employer = new Employer();
    employer.setCompanyName("HH");

    doInTransaction(() -> genericDao.save(employer));

    Employer savedEmployer = employerService.getById(employer.getId());
    assertEquals("HH", savedEmployer.getCompanyName());
  }

  @Test
  public void newEmployerWithVacanciesSavingTest() {
    Employer employer = new Employer();
    employer.setCompanyName("HH");

    Vacancy vacancy1 = new Vacancy(employer);
    vacancy1.setTitle("vacancy1");
    vacancy1.setDescription("All work and no play makes Jack a dull boy.");

    Vacancy vacancy2 = new Vacancy(employer);
    vacancy2.setTitle("vacancy2");
    vacancy2.setDescription("All work and no play makes Jack a dull boy.");

    employer.getVacancies().add(vacancy1);
    employer.getVacancies().add(vacancy2);
    doInTransaction(() -> genericDao.save(employer));

    Employer savedEmployer = employerService.getByIdPrefetched(employer.getId());
    assertEquals(2, savedEmployer.getVacancies().size());
  }

  @Test
  public void getJustCompanyName() {
    Employer employer = new Employer();
    employer.setCompanyName("HH");
    doInTransaction(() -> genericDao.save(employer));

    String companyName = employerService.getNameById(employer.getId());
    assertEquals("HH", companyName);
    assertEquals(1, getSelectCount());
  }

  @Test(expected = LazyInitializationException.class)
  public void shouldThrowLazyInitializationException() {
    Employer employer = new Employer();
    employer.setCompanyName("HH");

    Vacancy vacancy1 = new Vacancy(employer);
    vacancy1.setTitle("vacancy1");
    Vacancy vacancy2 = new Vacancy(employer);
    vacancy2.setTitle("vacancy2");

    employer.getVacancies().add(vacancy1);
    employer.getVacancies().add(vacancy2);

    doInTransaction(() -> genericDao.save(employer));

    Employer savedEmployer = employerService.getById(employer.getId());

    savedEmployer.getVacancies().get(0).getTitle();
  }

  @Test
  public void slowOperationTest() {
    Employer employer = new Employer();
    employer.setCompanyName("Bad Employer");

    Vacancy vacancy1 = new Vacancy(employer);
    vacancy1.setTitle("vacancy1");

    Vacancy vacancy2 = new Vacancy(employer);
    vacancy2.setTitle("vacancy2");

    employer.getVacancies().add(vacancy1);
    employer.getVacancies().add(vacancy2);

    doInTransaction(() -> genericDao.save(employer));

    employerService.blockIfEmployerUseBadWords(employer.getId());

    final Employer blockedEmployer = employerService.getByIdPrefetched(employer.getId());
    assertNotNull(blockedEmployer.getBlockTime());
    assertTrue(blockedEmployer.getVacancies().stream().noneMatch(v -> v.getArchivingTime() == null));

    assertEquals(2, getSelectCount());
  }

  @Test
  public void employerBlock() {
    Employer employer = new Employer();
    employer.setCompanyName("HH");

    Vacancy vacancy1 = new Vacancy(employer);
    vacancy1.setTitle("vacancy1");

    Vacancy vacancy2 = new Vacancy(employer);
    vacancy2.setTitle("vacancy2");

    employer.getVacancies().add(vacancy1);
    employer.getVacancies().add(vacancy2);
    doInTransaction(() -> genericDao.save(employer));

    employerService.block(employer.getId());

    final Employer blockedEmployer = employerService.getByIdPrefetched(employer.getId());
    assertNotNull(blockedEmployer.getBlockTime());
    assertTrue(blockedEmployer.getVacancies().stream().noneMatch(v -> v.getArchivingTime() == null));
    assertEquals(2, getSelectCount());
  }

}
