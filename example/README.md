# Example

### 🛠️ Requirements

* JDK Version >21

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

---

Also, pagination can now be easily implemented with JPA entities.

```
query {
  recentlyAddedFilms(
    last: 5,
  ) {
    edges {
      node {
        id
        title
        year
        synopsis
        studios {
          id
          name
        }
        directors {
          id
          firstname
          lastname
        }
        cast {
          id
          firstname
          lastname
        }
      }
      cursor
    }
    pageInfo {
      startCursor
      endCursor
      hasNextPage
      hasPreviousPage
    }
  }
}
```

---

New films can be added and also edited.

```
mutation {
  addFilm(input: {
    title: "Blade Runner 2049",
    year: 2017,
    synopsis: "Blade Runner 2049 is a 2017 American epic neo-noir science fiction film.",
    runtime: 163,
    studioIds: [],
    directorIds: [],
    castIds: []
  }) {
    id,
    title
  }
}
```

```
mutation {
  editFilm(input: {
    id: "d43543d8-cf91-4e8e-b0a8-c26eca651c6f",
    studioIds: ["800e55f1-b04c-4bb9-b8bf-dd1534dd7e92"],
    directorIds: ["7aea3641-d180-48d5-859e-55e76b0460db"],
    castIds: ["b4744225-4e48-4e84-ad52-aa30c10fb9d1", "fe1b66ad-ae3a-4895-9709-6d9153c97d0d"]
  }) {
    id,
    title
    studios {
      id
      name
    }
  }
}
```
