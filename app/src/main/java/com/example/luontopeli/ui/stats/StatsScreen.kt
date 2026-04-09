package com.example.luontopeli.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.luontopeli.data.local.entity.WalkSession
import com.example.luontopeli.viewmodel.WalkViewModel
import com.example.luontopeli.viewmodel.formatDistance
import com.example.luontopeli.viewmodel.formatDuration

@Composable
fun StatsScreen(walkViewModel: WalkViewModel = viewModel()) {
	val sessions by walkViewModel.sessions.collectAsState()

	LazyColumn(
		modifier = Modifier
			.fillMaxSize()
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp)
	) {
		item {
			Text(
				text = "Tilastot",
				style = MaterialTheme.typography.headlineSmall
			)
		}

		if (sessions.isEmpty()) {
			item {
				Card(
					colors = CardDefaults.cardColors(
						containerColor = MaterialTheme.colorScheme.surfaceVariant
					)
				) {
					Text(
						text = "Ei vielä tallennettuja kävelyitä. Aloita kävely karttanäkymästä.",
						modifier = Modifier.padding(16.dp),
						style = MaterialTheme.typography.bodyMedium
					)
				}
			}
		} else {
			item {
				SummaryCard(sessions)
			}

			item {
				Text(
					text = "Viimeisimmät kävelyt",
					style = MaterialTheme.typography.titleMedium
				)
			}

			items(sessions.take(20), key = { it.id }) { session ->
				SessionCard(session)
			}
		}
	}
}

@Composable
private fun SummaryCard(sessions: List<WalkSession>) {
	val totalSteps = sessions.sumOf { it.stepCount }
	val totalDistance = sessions.sumOf { it.distanceMeters.toDouble() }.toFloat()
	val totalDurationMillis = sessions.sumOf { (it.endTime ?: System.currentTimeMillis()) - it.startTime }
	val totalSpotsFound = sessions.sumOf { it.spotsFound }

	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer
		)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Text(
				text = "Yhteenveto",
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onPrimaryContainer
			)
			Spacer(modifier = Modifier.height(10.dp))

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text("Kävelyitä")
				Text(sessions.size.toString())
			}
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text("Askeleita")
				Text(totalSteps.toString())
			}
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text("Löytöjä")
				Text(totalSpotsFound.toString())
			}
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text("Matka")
				Text(formatDistance(totalDistance))
			}
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text("Aikaa")
				Text(formatDuration(0L, totalDurationMillis))
			}
		}
	}
}

@Composable
private fun SessionCard(session: WalkSession) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceVariant
		)
	) {
		Column(modifier = Modifier.padding(14.dp)) {
			Text(
				text = session.startTime.toFormattedDate(),
				style = MaterialTheme.typography.titleSmall
			)
			Spacer(modifier = Modifier.height(8.dp))

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text("Askeleet")
				Text(session.stepCount.toString())
			}
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text("Matka")
				Text(formatDistance(session.distanceMeters))
			}
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text("Löytöjä")
				Text(session.spotsFound.toString())
			}
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text("Kesto")
				Text(formatDuration(session.startTime, session.endTime ?: System.currentTimeMillis()))
			}
		}
	}
}

private fun Long.toFormattedDate(): String {
	val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
	return sdf.format(java.util.Date(this))
}