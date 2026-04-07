# BudgetFlow – Project Overview

## Šta je aplikacija?

BudgetFlow je Spring Boot REST API za praćenje ličnih finansija. Korisnici mogu kreirati miesečni budžet, rasporediti ga po kategorijama troškova, unositi transakcije (prihode i rashode) i generisati izvještaje o potrošnji.

## Tech Stack

| Komponenta | Tehnologija |
|---|---|
| Jezik | Java 25 |
| Framework | Spring Boot 4.0.2 |
| Baza | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Autentifikacija | JWT (jjwt 0.11.5) + Spring Security |
| Build | Maven |
| Validacija | Jakarta Bean Validation |
| Boilerplate | Lombok |

## Arhitektura

Projekat prati standardni Spring Boot layered architecture:

```
Controller → Service (Interface + Impl) → Repository → Database
```

Koriste se DTOs za transfer podataka između klijenta i servisa, a entiteti za ORM mapiranje.

## Moduli / Paketi

- **entity** – JPA entiteti: `User`, `Budget`, `BudgetCategory`, `Category`, `CategoryType`, `Transaction`, `User_report`
- **DTO** – Data Transfer Objects za request/response
- **controllers** – REST kontroleri (Auth, User, Budget, BudgetCategory, Category, CategoryType, Transaction, UserReport)
- **service** – Servisni interfejsi + implementacije
- **repository** – Spring Data JPA repozitoriji
- **security** – JWT filter, JWT utils, SecurityConfig
- **seeders** – Inicijalni podaci (CategoryType: ESSENTIAL, OPTIONAL, SAVINGS)
- **exception** – `CustomException` (RuntimeException wrapper)

## Baza podataka (ER model)

```
users ──< budget ──< budget_category >── category >── category_type
                                             │
users ──< transaction >─────────────────────┘
users ──< user_report
```

Dizajn je normalizovan do 3NF. `budget_category` je spojna tabela koja drži procentualnu raspodjelu budžeta po kategorijama (procenat + iznos). Kategorije imaju tip (ESSENTIAL / OPTIONAL / SAVINGS) što omogućava automatsku 50/30/20 raspodjelu.

## Ključne funkcionalnosti

- Registracija i prijava korisnika s JWT tokenima
- Kreiranje i upravljanje miesečnim budžetom
- Automatska 50/30/20 raspodjela budžeta po tipu kategorije
- Ručno definisanje procenta/iznosa po kategoriji
- Unos transakcija (prihod / rashod) s kategorijom i datumom
- Generisanje miesečnih finansijskih izvještaja
- Export transakcija u CSV format
- Seeder za inicijalne tipove kategorija pri pokretanju

## Šta je implementirano (u skladu s dokumentacijom)

✅ Registracija i login korisnika  
✅ CRUD korisnika (me endpoint)  
✅ Kreiranje i ažuriranje budžeta  
✅ Raspodjela budžeta po kategorijama (ručno + suggested 50/30/20)  
✅ Upravljanje kategorijama i tipovima  
✅ CRUD transakcija  
✅ Finansijski izvještaji i statistika po kategorijama  
✅ Export u CSV  
✅ BCrypt hashovanje lozinki  
✅ JWT autentifikacija  

## Šta nedostaje (planirano, nije implementirano)

❌ Promjena i oporavak lozinke  
❌ Import podataka putem Excel templateta  
❌ Notifikacije za neplaćene račune  
❌ Automatsko ažuriranje budžeta nakon unosa transakcije  
