# Blu Data JPA

Blu Data JPA is a powerful extension to Spring Data JPA for Spring Boot applications. It provides a simple and intuitive API to build dynamic database queries using filter models derived from your entity classes. Say goodbye to complex JPQL strings or verbose Criteria API code – just create filters and query!

## Key Features
- **Rich Filter Support**: Equal, NotEqual, Like, GreaterThan, LessThan, Between, In, IsNull, etc.
- **Logical Grouping**: AND, OR, NOT with nested groups
- **Nested Properties**: Query deep object graphs using dot notation (e.g., `owner.address.city`)
- **Pagination & Sorting**: Built-in support for paginated results with custom orders
- **Type-Safe**: Works seamlessly with Kotlin and Java entities
- **Easy CRUD**: Extend `BluDbService` for standard operations
- **Inheritance Support**: Full support for `@Inheritance` (SINGLE_TABLE) - query subclasses using `ClassFilter` or generic `cls` parameter

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

Define a base `Vehicle` entity using JPA SINGLE_TABLE inheritance with concrete subtypes:

```kotlin
import jakarta.persistence.*

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "vehicle_type", discriminatorType = DiscriminatorType.STRING)
open class Vehicle(
    @Id
    val id: Long = 0,
    val brand: String,
    val model: String,
    val fuelCapacity: Int
)

@Entity
@DiscriminatorValue("CAR")
class Car(
    id: Long = 0,
    brand: String,
    model: String,
    fuelCapacity: Int,
    val passengers: Int
) : Vehicle(id, brand, model, fuelCapacity)

@Entity
@DiscriminatorValue("TRUCK")
class Truck(
    id: Long = 0,
    brand: String,
    model: String,
    fuelCapacity: Int,
    val cargoWeight: Int
) : Vehicle(id, brand, model, fuelCapacity)

@Entity
@DiscriminatorValue("BUS")
class Bus(
    id: Long = 0,
    brand: String,
    model: String,
    fuelCapacity: Int,
    val passengersCount: Int
) : Vehicle(id, brand, model, fuelCapacity)
```

## Repository

Extend `BluRepositoryImpl` to create a repository for your entity:

```kotlin
import ir.bamap.blu.jpa.repository.BluRepositoryImpl
import org.springframework.stereotype.Repository

@Repository
class VehicleRepository : 
    BluRepositoryImpl<Vehicle, Long>(Vehicle::class.java) {
    // Optionally override methods like getIdProperty() if needed
}
```

## Service Layer (Recommended)

Extend `BluDbService` for ready-to-use CRUD operations:

```kotlin
import ir.bamap.blu.jpa.service.BluDbService
import org.springframework.stereotype.Service

@Service
class VehicleService(
    repository: VehicleRepository
) : BluDbService<Vehicle, Long>(repository)
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
lateinit var repository: VehicleRepository

// All vehicles with high fuel capacity
val highCapacity = repository.findBy(GreaterThan("fuelCapacity", 50))

// Cars using ClassFilter
val cars = repository.findBy(ClassFilter(Car::class.java))

// Trucks using cls parameter (typed result)
val trucks: List<Truck> = repository.findBy(Truck::class.java)

// Models containing "Toyota"
val matching = repository.findBy(Like("model", "%Toyota%"))

// Multiple filters (AND) - high capacity cars with many passengers
val highCapacityCars = repository.findBy(
    ClassFilter(Car::class.java),
    GreaterThan("fuelCapacity", 100),
    GreaterThan("passengers", 4)
)
```

### Pagination & Sorting

```kotlin
val filters = listOf(
    GreaterThanOrEqualTo("fuelCapacity", 100)
)
val orders = Orders(OrderModel("fuelCapacity", OrderModel.Direction.DESC))

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
// Trucks with high fuel capacity OR high cargo weight
val highCapacityOrCargoTrucks = repository.findBy(
    And(
        Or(
            GreaterThan("fuelCapacity", 200),
            GreaterThan("cargoWeight", 10000)
        ),
        ClassFilter(Truck::class.java)
    )
)

// First/Last - highest fuel capacity car
val fuelDescOrders = Orders(OrderModel("fuelCapacity", OrderModel.Direction.DESC))
val highestFuelCar: Car = repository.findFirst(fuelDescOrders, ClassFilter(Car::class.java))

// lowest fuel capacity vehicle
val fuelAscOrders = Orders(OrderModel("fuelCapacity", OrderModel.Direction.ASC))
val lowestFuelVehicle: Vehicle = repository.findFirst(fuelAscOrders)

// By IDs
val vehicles = repository.findByIds(listOf(1L, 2L, 3L))
```

### CRUD Examples (via Service)

```kotlin
@Autowired
lateinit var service: VehicleService

// Save
val newCar = Car(
    brand = "Toyota",
    model = "Camry",
    fuelCapacity = 60,
    passengers = 5
)
val saved = service.save(newCar)

// Delete
service.delete(saved)

// Find by ID
val found = service.find(saved.id)
```
