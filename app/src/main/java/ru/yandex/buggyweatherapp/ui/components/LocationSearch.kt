package ru.yandex.buggyweatherapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ru.yandex.buggyweatherapp.R

@Composable
fun LocationSearch(
    onCitySearch: (String) -> Unit,
    onLocationRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text(stringResource(R.string.search_city)) },
            placeholder = { Text(stringResource(R.string.search_city_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            leadingIcon = {
                IconButton(
                    onClick = { 
                        focusManager.clearFocus()
                        onLocationRequest() 
                    }
                ) {
                    Icon(
                        Icons.Default.LocationOn, 
                        contentDescription = context.getString(R.string.use_current_location)
                    )
                }
            },
            trailingIcon = {
                IconButton(
                    onClick = { 
                        if (searchText.isNotBlank()) {
                            focusManager.clearFocus()
                            onCitySearch(searchText)
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = context.getString(R.string.search)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { 
                    if (searchText.isNotBlank()) {
                        focusManager.clearFocus()
                        onCitySearch(searchText)
                    }
                }
            ),
            singleLine = true
        )
    }
}