# Example

### 🛠️ Requirements

* JDK Version >17

### 🚀 Start

```bash
cd application
./gradlew bootRun
```

### 📓 Usage

Visit http://localhost:8081/graphiql where one can run GraphQL queries.

One can then query with various selections which alter the SQL query on the fly depending on selected data & relations.

Following query executes multiple join statements.

```
query {
  companies {
    id
    name
    employees {
      id
      firstname
      lastname
      memberOf {
        id
        name
      }
    }
  }
}
```

However, this one features no join statements.

```
query {
  companies {
    id
    name 
  }
}
```

