package com.movie.app.best.data.model

data class CrewPerson(
    val id: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val category: String = "",
    val characters: String = ""
) {
    val isClickable: Boolean get() = id.isNotBlank()

    companion object {
        const val CAT_DIRECTOR = "Director"
        const val CAT_ACTOR = "Actor"
        const val CAT_OTHER = "Other"

        fun classifyCredit(category: String): String = when {
            category == "director" -> CAT_DIRECTOR
            category == "actor" || category == "actress" -> CAT_ACTOR
            else -> CAT_OTHER
        }

        fun fromCredit(credit: ImdbCredit): CrewPerson? {
            val cat = credit.category
            if (cat == "writer") return null
            if (credit.name.id.isBlank()) return null
            return CrewPerson(
                id = credit.name.id,
                displayName = credit.name.displayName,
                photoUrl = credit.name.photoUrl,
                category = classifyCredit(cat),
                characters = credit.characterText
            )
        }

        fun fromName(name: ImdbName, category: String): CrewPerson =
            CrewPerson(
                id = name.id,
                displayName = name.displayName,
                photoUrl = name.photoUrl,
                category = category
            )

        fun orderIndex(category: String): Int = when (category) {
            CAT_DIRECTOR -> 0
            CAT_ACTOR -> 1
            else -> 2
        }

        fun sortCrew(list: List<CrewPerson>): List<CrewPerson> {
            val seen = HashSet<String>()
            val result = mutableListOf<CrewPerson>()
            for (p in list) {
                if (p.id.isBlank() || seen.add(p.id)) result.add(p)
            }
            return result.sortedBy { orderIndex(it.category) }
        }
    }
}
