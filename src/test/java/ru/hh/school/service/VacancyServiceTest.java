package ru.hh.school.service;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.hh.school.BaseTest;
import ru.hh.school.TestHelper;
import ru.hh.school.dao.GenericDao;
import ru.hh.school.dao.VacancyDao;
import ru.hh.school.employers.StatisticsDto;
import ru.hh.school.entity.Area;
import ru.hh.school.entity.Employer;
import ru.hh.school.entity.Vacancy;
import ru.hh.school.util.TransactionHelper;

import static org.junit.Assert.assertEquals;

public class VacancyServiceTest extends BaseTest {
  private static VacancyService vacancyService;
  private static GenericDao genericDao;

  @BeforeClass
  public static void setupService() {
    genericDao = new GenericDao(sessionFactory);
    vacancyService = new VacancyService(
        new VacancyDao(sessionFactory),
        new TransactionHelper(sessionFactory)
    );
    TestHelper.executeScript(dataSource, "create_employers.sql");
  }

  @Test
  public void getSalaryStatistics() {
    TestHelper.executeScript(dataSource, "clear_data.sql");

    Employer employer = new Employer();
    employer.setCompanyName("HH");

    Area area = new Area();
    area.setName("Moscow");
    doInTransaction(()->genericDao.save(area));

    Vacancy vacancy1 = new Vacancy(employer);
    vacancy1.setTitle("vacancy1");
    vacancy1.setDescription("All work and no play makes Jack a dull boy.");
    vacancy1.setCompensationFrom(10);
    vacancy1.setCompensationTo(12);
    vacancy1.setArea(area);

    Vacancy vacancy2 = new Vacancy(employer);
    vacancy2.setTitle("vacancy2");
    vacancy2.setDescription("All work and no play makes Jack a dull boy.");
    vacancy2.setCompensationFrom(12);
    vacancy2.setCompensationTo(14);
    vacancy2.setArea(area);

    employer.getVacancies().add(vacancy1);
    employer.getVacancies().add(vacancy2);
    doInTransaction(()->genericDao.save(employer));

    StatisticsDto salaryStatistics = vacancyService.getSalaryStatistics(area);

    assertEquals(2, salaryStatistics.getVacancyCount());
    assertEquals(10, salaryStatistics.getMinSalary());
    assertEquals(14, salaryStatistics.getMaxSalary());

  }


}
