# Blu Data JPA

Blu Data JPA is a powerful extension to Spring Data JPA for Spring Boot applications. It provides a simple and intuitive API to build dynamic database queries using filter models derived from your entity classes. Say goodbye to complex JPQL strings or verbose Criteria API code – just create filters and query!

## Key Features
- **Rich Filter Support**: Equal, NotEqual, Like, GreaterThan, LessThan, Between, In, IsNull, etc.
- **Logical Grouping**: AND, OR, NOT with nested groups
- **Nested Properties**: Query deep object graphs using dot notation (e.g., `owner.address.city`)
- **Pagination & Sorting**: Built-in support for paginated results with custom orders
- **Type-Safe**: Works seamlessly with Kotlin and Java entities
- **Easy CRUD**: Extend `BluDbService` for standard operations

## Version Compatibility
Depending on the Spring Boot version you are using, you need to use this version of the library:

| Data JPA Library | Spring Boot | Kotlin | Java |
|------------------|-------------|--------|------|
| 4.0.x            | 4.0.0       | 2.2.21 | 17   |


## Installation

### Maven

```xml
<dependency>
    <groupId>ir.bamap.blu</groupId>
    <artifactId>data-jpa</artifactId>
    <version>4.0.0</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("ir.bamap.blu:data-jpa:4.0.0")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'ir.bamap.blu:data-jpa:4.0.0'
}
```

## Example Entity

Define a sample `Machine` entity with useful fields and an enum subtype:

```kotlin
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "machines")
data class Machine(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val name: String,

    val type: MachineType,

    val price: BigDecimal,

    val manufacturedAt: LocalDateTime,

    val isActive: Boolean = true
)

enum class MachineType {
    CNC_MACHINE,
    LATHE,
    MILLING_MACHINE
}
```

## Repository

Extend `BluRepositoryImpl` to create a repository for your entity:

```kotlin
import ir.bamap.blu.jpa.repository.BluRepositoryImpl
import org.springframework.stereotype.Repository

@Repository
class MachineRepository : 
    BluRepositoryImpl<Machine, Long>(Machine::class.java) {
    // Optionally override methods like getIdProperty() if needed
}
```

## Service Layer (Recommended)

Extend `BluDbService` for ready-to-use CRUD operations:

```kotlin
import ir.bamap.blu.jpa.service.BluDbService
import org.springframework.stereotype.Service

@Service
class MachineService(
    repository: MachineRepository
) : BluDbService<Machine, Long>(repository)
```

## Query Examples

Inject the repository or service and start querying. Assume necessary imports:

```kotlin
import ir.bamap.blu.model.*
import ir.bamap.blu.model.filter.*
import ir.bamap.blu.model.filter.Orders as Orders // Alias if needed
```

### Simple Queries

```kotlin
@Autowired
lateinit var repository: MachineRepository

// All active machines
val active = repository.findBy(IsNotNull("isActive"))

// By exact type
val cncs = repository.findBy(Equal("type", MachineType.CNC_MACHINE))

// Name contains "CNC"
val matching = repository.findBy(Like("name", "%CNC%"))

// Multiple filters (AND)
val expensiveActiveCncs = repository.findBy(
    Equal("type", MachineType.CNC_MACHINE),
    GreaterThan("price", BigDecimal("50000")),
    IsNotNull("isActive")
)
```

### Pagination & Sorting

```kotlin
val filters = listOf(
    GreaterThanOrEqualTo("manufacturedAt", LocalDateTime.of(2024, 1, 1, 0, 0))
)
val orders = Orders(OrderModel("price", OrderModel.Direction.DESC))

val searchModel = SearchModel(
    page = 0,
    limit = 20,
    filters = filters,
    orders = orders
)

val result = repository.findBy(searchModel)
println("Total: ${result.total}, Page size: ${result.records.size}")
```

### Advanced: Groups & Nested

```kotlin
// OR within AND
val highEndOrNew = repository.findBy(
    And(
        Or(
            GreaterThan("price", BigDecimal("100000")),
            GreaterThan("manufacturedAt", LocalDateTime.now().minusYears(1))
        ),
        Equal("type", MachineType.LATHE)
    )
)

// First/Last
val mostExpensive = repository.findFirst(
    OrderModel("price", OrderModel.Direction.DESC),
    Equal("type", MachineType.CNC_MACHINE)
)

val cheapest = repository.findLast(
    OrderModel("price", OrderModel.Direction.ASC)
)

// By IDs
val machines = repository.findByIds(listOf(1L, 2L, 3L))
```

### CRUD Examples (via Service)

```kotlin
@Autowired
lateinit var service: MachineService

// Save
val newMachine = Machine(name = "New CNC", type = MachineType.CNC_MACHINE, price = BigDecimal("60000"), manufacturedAt = LocalDateTime.now())
val saved = service.save(newMachine)

// Delete
service.delete(saved)

// Find by ID
val found = service.find(saved.id)
```
