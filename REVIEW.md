# BudgetFlow – Code Review

> Pregled stanja projekta, potencijalni problemi i prijedlozi poboljšanja.  
> Podijeljeno po prioritetu: 🔴 Kritično, 🟠 Važno, 🟡 Preporuka.

---

## 🔴 Kritični problemi (sigurnost)

### 1. JWT secret key je hardkodiran u kodu
**Fajl:** `security/JwtUtils.java`  
**Problem:** `"TajniKljucZaJWTKojiJeDuzine32Bajt+"` je direktno u izvornom kodu. Ako kod dospije na GitHub, secret je kompromitovan.  
**Rješenje:** Premjesti u `application.properties` (ili environment varijablu) i čitaj ga `@Value`.
```java
@Value("${jwt.secret}")
private String secretKey;
```

### 2. Lozinka baze podataka je hardkodirana
**Fajl:** `resources/application.properties`  
**Problem:** `spring.datasource.password=password` – ne smije ići u version control.  
**Rješenje:** Koristi environment varijable ili Spring profiles (`application-prod.properties`).

### 3. Gotovo sve rute su javno dostupne
**Fajl:** `security/SecurityConfig.java`  
**Problem:** Konfiguracija završava s `.anyRequest().permitAll()` – jedino `/api/category-types/**` zahtijeva JWT. Sve `/api/budgets/**`, `/api/transactions/**`, `/api/reports/**` su otvorene bez autentifikacije.  
**Rješenje:** Promijeniti u `.anyRequest().authenticated()`.

---

## 🟠 Važni problemi (ispravnost i stabilnost)

### 4. `UserRepository.findByUsername()` vraća `ScopedValue<T>`
**Fajl:** `repository/UserRepository.java`  
**Problem:** Metoda je deklarisana kao `ScopedValue<T> findByUsername(String username)`. `ScopedValue` je Java 21 concurrency API i nema veze s JPA repozitorijima. Ovo bi izazvalo runtime grešku.  
**Rješenje:** Promijeniti povratni tip u `Optional<User>`.

### 5. `BudgetServiceImpl.updateBudgetAfterTransaction()` je prazna metoda
**Fajl:** `service/BudgetServiceImpl.java`  
**Problem:** Metoda ne radi ništa – ključna logika (automatsko umanjivanje raspoloživog budžeta kategorije nakon unosa troška, core feature iz dokumentacije) nije implementirana.  
**Rješenje:** Implementirati logiku koja pronalazi `BudgetCategory` za datu kategoriju i mjesec i smanjuje `allocatedAmount`.

### 6. `getSummary()` i `getReportByUserAndMonth()` ignorišu `month` parametar
**Fajl:** `service/UserReportServiceImpl.java`  
**Problem:** `getSummary()` vraća sve transakcije korisnika bez filtriranja po miesecu. `getReportByUserAndMonth()` vraća samo prvu transakciju iz liste, ignoriše `month`.  
**Rješenje:** Dodati filtriranje transakcija po datumu (npr. `transaction.getDate().toString().startsWith(month)`).

### 7. `User_report` nema pravu vezu s korisnikom
**Fajl:** `entity/User_report.java`  
**Problem:** Entitet čuva `private Long userId` kao sirov `Long` umjesto JPA `@ManyToOne` relacije. Ovo nije konzistentno s ostalim entitetima i ORM best practice.  
**Rješenje:** Zamijeniti s `@ManyToOne private User user;` (kao što je urađeno u `Transaction`, `Budget`, itd.).

### 8. `categoryBreakdown` u `User_report` čuva JSON kao String
**Fajl:** `entity/User_report.java`  
**Problem:** `@Lob private String categoryBreakdown` je anti-pattern. Teško za upite, nema type safety, teško za promjenu strukture.  
**Rješenje:** Kreirati posebnu tabelu `user_report_category` s relacijom, ili koristiti PostgreSQL `jsonb` tip kolone.

### 9. `Transaction.type` je plain String umjesto enum
**Fajl:** `entity/Transaction.java`  
**Problem:** Nema zaštite od pogrešnih vrijednosti – `"income"`, `"Expense"`, `"RASХОД"` su svi validni za JPA. Poređenje se radi case-insensitive (`equalsIgnoreCase`) na više mjesta.  
**Rješenje:** Kreirati `enum TransactionType { INCOME, EXPENSE }` i koristiti `@Enumerated(EnumType.STRING)`.

### 10. `ddl-auto=update` je opasno u produkciji
**Fajl:** `resources/application.properties`  
**Problem:** Hibernate može automatski promijeniti shemu baze (dodavati kolone), ali ne može ih brisati. U produkciji ovo može dovesti do nekonzistentnosti.  
**Rješenje:** Koristiti Flyway ili Liquibase za migracije. Za razvoj je `update` ok, ali za prod treba `validate`.

### 11. Nema `@Transactional` anotacija na servisnim metodama
**Fajl:** svi `*ServiceImpl.java` fajlovi  
**Problem:** Metode koje pišu u bazu nisu anotisane s `@Transactional`. Ako dođe do greške na sredini operacije (npr. pri `applySuggestedBudget` koji sprema više redova), nema rollbacka.  
**Rješenje:** Dodati `@Transactional` na metode koje vrše više write operacija.

### 12. Poslovna logika je duplirana između Controllera i Servicea
**Fajl:** `controllers/BudgetController.java` vs `service/BudgetServiceImpl.java`  
**Problem:** 50/30/20 logika i logika za ručno dodavanje kategorije postoje identično u oba fajla. Controller ne bi trebao sadržavati poslovnu logiku.  
**Rješenje:** Controller treba samo da prima request i delegira servisu. Ukloniti poslovnu logiku iz kontrolera i pozvati servisne metode koje već postoje.

---

## 🟡 Preporuke (code quality i best practices)

### 13. Nedosljedan stil entiteta – Lombok se ne koristi svuda
**Problem:** `Transaction`, `User_report` i `BudgetCategory` imaju ručno pisane getere, setere i konstruktore, dok `User`, `Budget`, `Category` koriste Lombok. Projekt je već uvezao Lombok – treba ga koristiti konzistentno.  
**Rješenje:** Dodati `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` na sve entitete.

### 14. `User_report` krši Java naming convention
**Problem:** Ime klase `User_report` koristi snake_case. Java konvencija je PascalCase.  
**Rješenje:** Preimenovati u `UserReport` (uz refaktorisanje svih referenci).

### 15. `CategoryTypeSeeder` ima pogrešnu anotaciju
**Fajl:** `seeders/CategoryTypeSeeder.java`  
**Problem:** Klasa ima `@SpringBootApplication` anotaciju uz `@Component`. `@SpringBootApplication` nije namijenjena za komponente – može izazvati neočekivano ponašanje pri skeniranju.  
**Rješenje:** Ukloniti `@SpringBootApplication`, ostaviti samo `@Component`.

### 16. Nema globalnog exception handlera
**Problem:** `CustomException` se baca ali nema `@ControllerAdvice` koji bi je uhvatio i vratio strukturiran JSON odgovor s odgovarajućim HTTP statusom. Trenutno vjerovatno vraća 500 umjesto 404/400.  
**Rješenje:** Kreirati `GlobalExceptionHandler` klasu s `@ControllerAdvice` i `@ExceptionHandler`.

### 17. JWT biblioteka je zastarjela verzija
**Fajl:** `pom.xml`  
**Problem:** `jjwt` verzija `0.11.5` – aktualna je `0.12.x` koja ima novi, čišći API (bez deprecated `parserBuilder()` i `SignatureAlgorithm`).  
**Rješenje:** Ažurirati na `0.12.6` i prilagoditi API pozive.

### 18. `show-sql=true` ne bi trebalo biti u produkciji
**Fajl:** `resources/application.properties`  
**Problem:** Ispisuje sve SQL upite u konzolu – performansno loše i potencijalno otkriva strukturu baze.  
**Rješenje:** Premjestiti u `application-dev.properties` profil.

### 19. Export CSV nema ispravne HTTP headere
**Fajl:** `controllers/UserReportController.java`, `service/UserReportServiceImpl.java`  
**Problem:** `exportReportToCSV` vraća `byte[]` ali response nema `Content-Type: text/csv` ni `Content-Disposition: attachment; filename=...` header.  
**Rješenje:** Koristiti `HttpHeaders` u kontroleru da postavi ispravne headere.

### 20. Nema CORS konfiguracije
**Problem:** Ako postoji ili se planira frontend (React/Angular/Vue), bez CORS konfiguracije browser će blokirati sve zahtjeve s drugog origina.  
**Rješenje:** Dodati `@CrossOrigin` na kontrolere ili globalnu CORS konfiguraciju u `SecurityConfig`.

### 21. `open-in-view` nije eksplicitno isključen
**Fajl:** `resources/application.properties`  
**Problem:** Spring Boot po defaultu drži Hibernate sesiju otvorenu tokom cijelog HTTP requesta (`open-in-view=true`). Ovo može sakriti N+1 probleme i potencijalnu degradaciju performansi.  
**Rješenje:** Dodati `spring.jpa.open-in-view=false`.

### 22. `BudgetController` prima `userId` iz DTO-a umjesto iz JWT-a
**Problem:** Korisnik šalje `userId` u request body-u. Maliciozni korisnik može proslijediti tuđi `userId` i pristupiti tuđim podacima.  
**Rješenje:** Uvijek dohvatati trenutnog korisnika iz `SecurityContext` (`userService.getCurrentUser()`), nikad iz request parametara.

### 23. Nedostaju unit testovi
**Fajl:** `src/test/`  
**Problem:** Postoji samo prazna `BudgetFlowApplicationTests` klasa. Nema testova za servisnu logiku (50/30/20 raspodjelu, validaciju budžeta, itd.).  
**Rješenje:** Dodati JUnit 5 + Mockito testove za barem `BudgetService` i `UserReportService`.

---

## Sažetak stanja

| Kategorija | Ocjena |
|---|---|
| Tehnologije (Java 25, Spring Boot 4) | ✅ Odlično |
| Arhitektura (layered, interface+impl) | ✅ Dobro |
| Sigurnost | 🔴 Potrebni hitni ispravci |
| Ispravnost logike | 🟠 Nekoliko neimplementiranih metoda |
| Kvalitet koda | 🟡 Nedosljednost u stilu |
| Testovi | 🔴 Ne postoje |
| Baza podataka (shema, 3NF) | 🟠 Dobra osnova, 2-3 stvari za popraviti |
