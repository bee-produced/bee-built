package com.beeproduced.bee.persistent

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-06-22
 */
@Suppress("unused")
class IncubatingTest {

    // Following examples from the user service showcase limitations of
    // the load graph api.
    // Recursive queries where a property is requested in a subquery with additional fields loaded
    // will not load this files if the property is queried beforehand
    // e.g. User -> CompanyMember -> Company (with no addresses) -> User -> CompanyMember -> Company (with addresses)
    // Addresses will not be loaded when it is the same entity from before because it was already queried

    /*fun customSelectById(id: UserId): User? {
        val query = queryFactory.selectQuery<User> {
            select(entity(User::class))
            from(entity(User::class))
            where(column(User::id).equal(id))
        }

        // val graph = entityManager.createEntityGraph(User::class.java)
        // val s1 = graph.addSubgraph(User::memberOf.name, CompanyMember::class.java)
        // // Graph to test
        // val s2 = s1.addSubgraph(CompanyMember::company.name, Company::class.java)
        // val s3 = s2.addSubgraph(Company::employees.name, CompanyMember::class.java)
        // // Query works also without this 🤔
        // val s4 = s3.addSubgraph(CompanyMember::user.name, User::class.java)
        // val s5 = s4.addSubgraph(User::memberOf.name, CompanyMember::class.java)
        // val s6 = s5.addSubgraph(CompanyMember::company.name, Company::class.java)
        // -- Alternative test --
        // val s2 = s1.addSubgraph(CompanyMember::company.name, Company::class.java)
        // s2.addAttributeNodes(Company::billingAddress.name, Company::shippingAddress.name)

        // ====
        // query {
        //     user(id: "1d207bce-f951-47f7-b732-fd35700b9d55") {
        //     id,
        //     firstName,
        //     lastName,
        //     memberOf {
        //         id,
        //         legalName
        //         employees {
        //             id,
        //             email,
        //             firstName,
        //             lastName
        //             memberOf {
        //                 id
        //                 legalName
        //                 employees {
        //                         id,
        //                         email,
        //                         firstName,
        //                         lastName
        //                 }
        //             }
        //         }
        //     }
        // }
        // }
        // Translated query fails to resolve last memberOf, is always null
        val unwrappedEntityManager = (entityManager as EntityManagerProxy).targetEntityManager
        val graph = GraphParser.parse(
            User::class.java,
            "memberOf(company(employees(user(memberOf(company(employees(user)))))))",
            unwrappedEntityManager
        )

        query.setHint("jakarta.persistence.loadgraph", graph)
        val users = query.resultList
        Unproxy.unproxyEntities(users)
        entityManager.clear()

        return users.firstOrNull()
    }*/

}