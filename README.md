# modular-chat-api

Uzun süredir .NET ile backend yazıyorum, bir süredir de Java + Spring Boot'a geçiş yapıyorum. Bu repo o geçişin parçası. Bildiğim bir şeyi (JWT'li auth + SignalR'lı chat) bu sefer Spring tarafında yazıp farkları kendi elimle görmek istedim.

Yani bu bir "bakın ne güzel proje" reposu değil, daha çok öğrenme defteri. Kodun içinde bol bol `.NET'teki X karşılığı` diye yorum göreceksiniz, onlar kendime not.

## Ne yapıyor

- Kayıt ol / giriş yap (JWT)
- Birebir ve grup sohbet açma
- WebSocket (STOMP) üzerinden anlık mesajlaşma
- "yazıyor..." bilgisi, kim çevrimiçi kim değil
- Mesaj geçmişi (sayfalı)

## Yapı

Modüler monolit denedim. Tek uygulama ama içeride modüller birbirinin içine girmiyor:

```
common/        -> security, websocket config, global hata yakalama
modules/auth   -> kullanıcı, kayıt, login
modules/chat   -> sohbet, mesaj, socket controller'ları
```

Chat modülü `User` entity'sini hiç bilmiyor. Kullanıcı bilgisine ihtiyacı olunca auth modülünün açtığı `UserQueryApi` interface'ine soruyor. Mesajlarda da sadece `senderId` tutuluyor, entity'ler arasında ilişki (navigation property) yok. İleride modülü ayırmak istersem acısız olsun diye.

## .NET'ten gelince kafamı karıştıranlar

Kendime yazdığım küçük sözlük:

| .NET | Spring |
|---|---|
| `Program.cs` / `builder.Services.Add...` | `@Configuration` sınıfları + `@Bean` |
| `AddScoped/AddSingleton` | `@Service`, `@Component` (default singleton) |
| Middleware | `Filter` (`OncePerRequestFilter`) |
| `AddJwtBearer` | kendi yazdığım `JwtAuthenticationFilter` |
| `[Authorize]` | `SecurityConfig` içinde `.authenticated()` |
| DataAnnotations | `jakarta.validation` (`@NotBlank`, `@Email`) + `@Valid` |
| EF Core `DbContext` | Spring Data JPA repository interface'leri |
| LINQ sorgusu | metot isminden sorgu (`findByEmail`) ya da `@Query` |
| `IHubContext<ChatHub>` | `SimpMessagingTemplate` |
| SignalR Hub | `@MessageMapping` olan `@Controller` |
| `Clients.Group(...)` | `/topic/...` |
| `Clients.User(...)` | `/user/queue/...` |
| `OnConnectedAsync` | `SessionConnectedEvent` dinleyicisi |
| `UseExceptionHandler` | `@RestControllerAdvice` |

En çok alışmam gerekenler:

- **Lombok.** Java'da property yok, getter/setter elle yazılıyor. Lombok `@Getter @Setter @Builder` ile bunları derlemede üretiyor. Başta büyü gibi geldi.
- **Id tipi.** .NET'te hep `Guid` kullanıyordum, burada çoğu örnekte `Long` + `IDENTITY` var, ben de öyle yaptım. İstersem `UUID` de kullanabilirim, Java'da karşılığı o.
- **Middleware sırası.** .NET'te `app.Use...` sırası her şey. Spring'de filter'ları security zincirine `addFilterBefore(...)` ile koyuyorsun.
- **Socket.** SignalR gibi hazır bir şey yok, STOMP diye bir protokol üstünden gidiyor. Token'ı da handshake'te değil, STOMP'un CONNECT mesajında header olarak gönderiyorsun.

## Farklı sandım, aslında aynıymış

İlk bakışta her şey yabancı geldi ama kurcaladıkça çoğu şeyin sadece adının değiştiğini gördüm:

- **Filter = middleware.** `chain.doFilter()` bildiğin `await next()`. İsteği al, bir şey yap, sıradakine pasla. Mantık birebir aynı.
- **`final` = `readonly`.** Constructor injection da aynı. Lombok sadece ctor'u benim yerime yazıyor.
- **`@Service` = `AddScoped` ile kayıt.** Spring sınıfı kendisi bulup DI'ye ekliyor, ben de ctor'dan alıyorum. Tek fark varsayılan ömrün singleton olması.
- **`SecurityContextHolder` = `HttpContext.User`.** Filter'da kullanıcıyı içine koyuyorum, controller'da `Principal` olarak geri alıyorum.
- **`@Transactional` = `SaveChanges` + transaction.** EF'teki change tracker burada da var (dirty checking). Entity'yi değiştirip bırakıyorsun, method bitince kaydediliyor.
- **`@RestControllerAdvice` = exception middleware.** Hatayı tek yerde yakalayıp düzgün response dönüyorsun.
- **STOMP + `@MessageMapping` = SignalR Hub.** Topic gruba göndermek, `/user/queue` kullanıcıya özel göndermek. `SimpMessagingTemplate` de `IHubContext`'in aynısı.
- **`Long` id = `int` id.** Guid istersem `UUID` var, onu da aynı şekilde kullanabiliyorum.

Kısacası öğrenilecek yeni bir şeyden çok yeni isimler var.

## Repository'de sorgu yazma: iki yol

**1. Metot isminden.** Burada hiç sorgu yazmıyorsun, Spring metot isminden kendisi çıkarıyor:

```java
boolean existsByEmail(String email);                        // SELECT COUNT(*) > 0 ... WHERE email = ?
Optional<User> findByEmail(String email);                   // WHERE email = ?
Page<ChatMessage> findByConversationIdOrderBySentAtDesc(...); // WHERE ... ORDER BY sent_at DESC
```

Bir arkadaşın söylediği güzel bir şey vardı: entity içinde başka bir entity tutuyorsan (ilişki kurduysan) onun alanlarına da isimle ulaşabiliyorsun. Mesela mesajın içinde `Conversation conversation` olsaydı şöyle yazabilirdim:

```java
List<ChatMessage> findByConversation_Code(String code);  // join'i kendisi atıyor
boolean existsByConversation_Code(String code);
```

Ben chat modülünde entity'ler arasında ilişki kurmadım (sadece id tutuyorum), o yüzden bende `findByConversationId` gibi düz alan üzerinden gidiyor.

Kendime kısa kopya kağıdı (metot isminde kullanılabilen kelimeler):

| Başlangıç | Ne yapar |
|---|---|
| `findBy...` / `getBy...` / `readBy...` | kayıt getirir |
| `existsBy...` | var mı yok mu, `boolean` döner (LINQ `Any`) |
| `countBy...` | kaç tane, `long` döner (LINQ `Count`) |
| `deleteBy...` | siler (`@Transactional` ister) |
| `findFirstBy...` / `findTop10By...` | ilk 1 / ilk 10 kayıt (`Take`) |
| `findDistinctBy...` | tekrarsız (`Distinct`) |

| Şart kelimesi | SQL karşılığı |
|---|---|
| `findByEmail` | `email = ?` |
| `findByEmailAndRole` | `email = ? AND role = ?` |
| `findByEmailOrUsername` | `email = ? OR username = ?` |
| `findByAgeGreaterThan` / `LessThan` | `age > ?` / `age < ?` |
| `findBySentAtBetween` | `sent_at BETWEEN ? AND ?` |
| `findBySentAtAfter` / `Before` | tarih sonra / önce |
| `findByNameContaining` | `LIKE %?%` (`Contains`) |
| `findByNameStartingWith` / `EndingWith` | `LIKE ?%` / `LIKE %?` |
| `findByNameIgnoreCase` | büyük/küçük harf duyarsız |
| `findByNameIsNull` / `IsNotNull` | `IS NULL` / `IS NOT NULL` |
| `findByIdIn(List<Long>)` | `id IN (...)` |
| `findByActiveTrue` / `False` | `active = true` / `false` |
| `findByNameNot` | `name <> ?` |
| `...OrderBySentAtDesc` | `ORDER BY sent_at DESC` |
| `findByConversation_Code` | ilişkili entity'nin alanı (join) |

Parametre sırası isimdeki sıraya göre gidiyor. Sonuna `Pageable` eklersen sayfalama da yapıyor.

**2. `@Query` ile.** İsim çok uzayınca ya da isimle anlatılamayınca kendin yazıyorsun:

```java
@Query("select c from Conversation c where c.type = :type and :a member of c.participantIds")
Optional<Conversation> find(@Param("type") ConversationType type, @Param("a") Long userA);
```

Buradaki `:type` ve `:a` isimli parametre. Başındaki iki nokta "bu bir değişken, değerini dışarıdan vereceğim" demek. `@Param("type")` de metot parametresini sorgudaki `:type` ile eşleştiriyor. .NET'te Dapper'da `@type` yazıp `new { type = ... }` geçmenin aynısı, sadece `@` yerine `:` var. Değerler string olarak yapıştırılmadığı için SQL injection riski de yok.

`c.type = :type` da "sohbetin tipi, verdiğim tipe eşit olsun" demek. `c`, `Conversation c` diye verdiğim kısa ad, LINQ'teki `c => c.Type == type` içindeki `c` gibi.

`:a member of c.participantIds` ise "a, sohbetin katılımcı listesinin içinde mi" demek. LINQ'teki `c.ParticipantIds.Contains(a)` ile aynı şey.

## Entity içinde liste tutmak (`@ElementCollection`)

Sohbette kimlerin olduğunu `Set<Long> participantIds` diye tutuyorum. Veritabanında bir kolona liste koyamadığın için JPA bunu ayrı bir tabloya yazıyor:

```java
@ElementCollection(fetch = FetchType.EAGER)   // bu bir entity değil, basit değerlerden oluşan liste
@CollectionTable(name = "conversation_participants",               // ayrı tablonun adı
                 joinColumns = @JoinColumn(name = "conversation_id")) // sohbete bağlayan kolon
@Column(name = "user_id")                     // değerlerin yazıldığı kolon
private Set<Long> participantIds = new HashSet<>();
```

Sonuçta şöyle bir tablo oluşuyor:

| conversation_id | user_id |
|---|---|
| 1 | 5 |
| 1 | 7 |

EF Core'da bunun için ayrı bir `ConversationParticipant` entity'si yazıp `HasMany` ile bağlardım. Burada entity yazmadan JPA hallediyor. `EAGER` "sohbeti çekerken katılımcıları da hemen getir" demek, EF'teki `Include` gibi. `@Builder.Default` da Lombok builder'ı kullanınca listenin `null` değil boş gelmesi için.

## Çalıştırma

PostgreSQL'de `chat_db` diye bir veritabanı aç, `application.yaml` içindeki şifreyi kendininkiyle değiştir:

```bash
./mvnw spring-boot:run
```

- Swagger: http://localhost:8080/swagger-ui.html
- Basit test sayfası: http://localhost:8080/ (iki ayrı sekmede iki kullanıcıyla girip yazışabilirsin)

Not: Bende Windows + Java 26'da Tomcat "Unable to establish loopback connection" hatası verdi. `-Djdk.net.unixdomain.tmpdir=C:/Temp` parametresini ekleyince düzeldi. Java 21'de muhtemelen bu sorun yok.

## Socket kullanımı (kısaca)

Bağlan: `ws://localhost:8080/ws`, CONNECT header'ı `Authorization: Bearer <token>`

| Ne | Nereye |
|---|---|
| Mesaj gönder | `/app/chat.send` → `{ "conversationId": 1, "content": "selam" }` |
| Yazıyor bilgisi | `/app/chat.typing` → `{ "conversationId": 1, "typing": true }` |
| Sohbeti dinle | `/topic/conversations/{id}` |
| Yazıyor'u dinle | `/topic/conversations/{id}/typing` |
| Bildirimler | `/user/queue/notifications` |
| Hatalar | `/user/queue/errors` |
| Online/offline | `/topic/presence` |

## Sonra yapmak istediklerim

- Refresh token
- Okundu bilgisi
- Simple broker yerine RabbitMQ/Redis, birden fazla instance çalışabilsin diye
- Spring Modulith'e bakmak
