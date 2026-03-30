package com.example.luontopeli.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room-tietokantaentiteetti yksittäiselle kävelykerralle.
 *
 * @property id             Uniikki tunniste (UUID), generoidaan automaattisesti
 * @property startTime      Kävelyn aloitusaika millisekunteina (epoch)
 * @property endTime        Kävelyn lopetusaika millisekunteina (null jos käynnissä)
 * @property stepCount      Mitattujen askelien lukumäärä
 * @property distanceMeters Arvioitu kuljettu matka metreinä (askeleet * askelpituus)
 * @property spotsFound     Kävelyn aikana löydettyjen luontokohteiden määrä
 * @property isActive       Onko kävely parhaillaan käynnissä
 */
@Entity(tableName = "walk_sessions")
data class WalkSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val stepCount: Int = 0,
    val distanceMeters: Float = 0f,
    val spotsFound: Int = 0,
    val isActive: Boolean = true
)