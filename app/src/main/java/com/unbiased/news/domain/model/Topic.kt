package com.unbiased.news.domain.model

data class Topic(
    val id: String,
    val name: String,
    val description: String? = null
)

object DefaultTopics {
    val TECH = Topic("tech", "Technology")
    val POLITICS = Topic("politics", "Politics")
    val ECONOMY = Topic("economy", "Economy")
    val WORLD = Topic("world", "World News")
    val SCIENCE = Topic("science", "Science")
    val HEALTH = Topic("health", "Health")
    val CULTURE = Topic("culture", "Culture")

    val ALL = listOf(TECH, POLITICS, ECONOMY, WORLD, SCIENCE, HEALTH, CULTURE)
}
