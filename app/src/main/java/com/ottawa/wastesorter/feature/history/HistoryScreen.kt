package com.ottawa.wastesorter.feature.history

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.appContainer
import com.ottawa.wastesorter.domain.model.HistoryItem
import com.ottawa.wastesorter.domain.repository.HistoryRepository
import com.ottawa.wastesorter.ui.components.CapsLabel
import com.ottawa.wastesorter.ui.components.ItemRow
import com.ottawa.wastesorter.ui.components.PrimaryButton
import com.ottawa.wastesorter.ui.components.ScreenTitle
import com.ottawa.wastesorter.ui.theme.Ows
import com.ottawa.wastesorter.ui.theme.OwsType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {
    val query = MutableStateFlow("")

    val items: StateFlow<List<HistoryItem>?> = query
        .flatMapLatest { repository.observe(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.delete(id) }
    }
}

private enum class Day { TODAY, YESTERDAY, EARLIER }

private fun dayOf(time: Long): Day {
    val start = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return when {
        time >= start -> Day.TODAY
        time >= start - DateUtils.DAY_IN_MILLIS -> Day.YESTERDAY
        else -> Day.EARLIER
    }
}

/** Écran 09 — Historique (groupé par jour) et état S7 — historique vide. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onOpen: (HistoryItem) -> Unit, onTakePhoto: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val vm: HistoryViewModel = viewModel(factory = viewModelFactory { initializer { HistoryViewModel(context.appContainer.history) } })
    val items by vm.items.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val loaded = items

    Column(modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
        ScreenTitle(stringResource(R.string.history_title), Modifier.padding(top = 8.dp, bottom = 20.dp))
        when {
            loaded == null -> Unit
            loaded.isEmpty() && query.isBlank() -> EmptyHistory(onTakePhoto)
            else -> HistoryList(loaded, query, onQuery = { vm.query.value = it.take(60) }, onOpen = onOpen, onDelete = vm::delete)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryList(
    items: List<HistoryItem>,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (HistoryItem) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(Ows.Surface)
            .border(1.dp, Ows.Border, RoundedCornerShape(26.dp)).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.Search, null, tint = Ows.Secondary, modifier = Modifier.size(22.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(stringResource(R.string.history_search), style = OwsType.BodySmall, color = Ows.Muted)
            BasicTextField(
                value = query, onValueChange = onQuery, singleLine = true,
                textStyle = OwsType.BodySmall.copy(color = Ows.Charcoal), cursorBrush = SolidColor(Ows.Green),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (items.isEmpty()) {
        Text(
            stringResource(R.string.history_no_match), style = OwsType.BodySmall, color = Ows.Secondary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        )
        return
    }
    val groups = items.groupBy { dayOf(it.createdAt) }.toSortedMap()
    val firstDay = groups.firstKey()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEach { (day, dayItems) ->
            item(key = "label-$day") {
                val label = when (day) {
                    Day.TODAY -> R.string.history_today
                    Day.YESTERDAY -> R.string.history_yesterday
                    Day.EARLIER -> R.string.history_earlier
                }
                CapsLabel(stringResource(label), Modifier.padding(top = if (day == firstDay) 0.dp else 12.dp))
            }
            items(dayItems, key = { it.id }) { item ->
                val dismiss = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
                    val remove = v == SwipeToDismissBoxValue.EndToStart
                    if (remove) onDelete(item.id)
                    remove
                })
                SwipeToDismissBox(
                    state = dismiss,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Ows.Danger).padding(horizontal = 24.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) { Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete), tint = Color.White) }
                    },
                ) {
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Ows.Surface).border(1.dp, Ows.Border, RoundedCornerShape(20.dp))) {
                        ItemRow(
                            title = item.itemName,
                            material = item.outcome.primaryMaterial,
                            destinations = item.outcome.classification.destinations,
                            onClick = { onOpen(item) },
                            photoPath = item.outcome.thumbnailPath,
                            trailingText = if (day == Day.EARLIER) {
                                DateFormat.getMediumDateFormat(context).format(item.createdAt)
                            } else {
                                DateFormat.getTimeFormat(context).format(item.createdAt)
                            },
                            showChevron = false,
                        )
                    }
                }
            }
        }
        item {
            Text(
                stringResource(R.string.history_swipe_hint), style = OwsType.Meta.copy(fontSize = 12.sp), color = Ows.Muted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun EmptyHistory(onTakePhoto: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(Ows.GreenSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.History, null, tint = Ows.Green, modifier = Modifier.size(42.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.history_empty_title), style = OwsType.CardTitle.copy(fontSize = 20.sp), color = Ows.Charcoal)
            Text(
                stringResource(R.string.history_empty), style = OwsType.BodySmall, color = Ows.Secondary,
                textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 280.dp),
            )
        }
        PrimaryButton(stringResource(R.string.home_take_photo), onTakePhoto, icon = Icons.Rounded.PhotoCamera)
    }
}
