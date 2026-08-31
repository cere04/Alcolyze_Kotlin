package com.example.alcolyze.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

data class BestPracticeItem(val title: String, val description: String)

private val COSA_FARE = listOf(
    BestPracticeItem(
        "Mangia prima e durante il consumo",
        "Il cibo nello stomaco, specialmente se ricco di carboidrati complessi e grassi, agisce come una barriera. Rallenta lo svuotamento gastrico e fa sì che l'alcol venga assorbito nel sangue in modo molto più graduale, evitando picchi alcolemici improvvisi."
    ),
    BestPracticeItem(
        "Alterna ogni drink con un bicchiere d'acqua",
        "L'alcol è un potente diuretico che causa una rapida disidratazione (la causa principale dei sintomi del doposbronza). Bere acqua tra un drink e l'altro mantiene il corpo idratato, protegge il fegato e ti aiuta a rallentare naturalmente il ritmo delle consumazioni."
    ),
    BestPracticeItem(
        "Rispetta i tempi del tuo metabolismo",
        "In media, il fegato umano impiega circa un'ora per metabolizzare una singola unità alcolica (l'equivalente di una birra piccola, un calice di vino o uno shottino). Distanziare le bevute dà al tuo corpo il tempo necessario per smaltire le tossine senza sovraccaricarsi."
    ),
    BestPracticeItem(
        "Usa l'app per tracciare ciò che bevi",
        "La percezione soggettiva di quanto si è bevuto o di quanto si è \"lucidi\" è spesso ingannevole a causa degli effetti disinibenti dell'alcol. Inserire ogni consumazione nell'app ti restituisce un dato oggettivo e matematico sul tuo stato reale."
    ),
    BestPracticeItem(
        "Pianifica il ritorno a casa in anticipo",
        "Prendi la decisione su come tornare a casa prima di iniziare a bere. Che si tratti di nominare un \"guidatore designato\" (chi guida non beve), prenotare un taxi o controllare gli orari dei mezzi pubblici, farlo da sobri previene scelte pericolose a fine serata."
    ),
    BestPracticeItem(
        "Ascolta i segnali del tuo corpo",
        "Il picco di concentrazione di alcol nel sangue (BAC) si raggiunge dai 30 ai 90 minuti dopo aver finito di bere. Se inizi a sentirti alterato, fermati: gli effetti dell'ultimo bicchiere che hai bevuto devono ancora manifestarsi completamente."
    )
)

private val COSA_NON_FARE = listOf(
    BestPracticeItem(
        "Non bere a stomaco vuoto",
        "Senza cibo, l'alcol passa quasi istantaneamente dallo stomaco all'intestino tenue, venendo assorbito nel flusso sanguigno in tempi rapidissimi. Questo causa una \"botta\" immediata e un picco tossico che il fegato fatica a gestire."
    ),
    BestPracticeItem(
        "Non mescolare alcol ed energy drink",
        "Le bevande energetiche contengono alte dosi di caffeina e stimolanti che mascherano gli effetti sedativi dell'alcol. Questo \"inganna\" il cervello facendoti sentire sobrio e vigile, spingendoti a bere quantità molto superiori a quelle che il tuo corpo può tollerare."
    ),
    BestPracticeItem(
        "Non affidarti ai \"falsi miti\" per tornare sobrio",
        "Caffè forte, docce ghiacciate, bibite energetiche o corse al freddo non abbassano il tasso alcolemico. L'unico fattore in grado di eliminare l'alcol dal sangue è il tempo necessario al fegato per fare il suo lavoro."
    ),
    BestPracticeItem(
        "Non fare \"Binge Drinking\" (bere per ubriacarsi)",
        "Assumere 4-5 drink o più in un arco di tempo brevissimo (come nei classici \"giochi alcolici\") satura istantaneamente gli enzimi epatici. L'alcol non processato si riversa nel sangue, aumentando drasticamente il rischio di intossicazione acuta e coma etilico."
    ),
    BestPracticeItem(
        "Non mescolare mai alcol e farmaci",
        "Molti farmaci da banco (come antinfiammatori o paracetamolo) e farmaci con prescrizione (come antibiotici, antistaminici o psicofarmaci) interagiscono in modo pericoloso con l'alcol. Possono moltiplicarne l'effetto tossico, causare danni al fegato o provocare gravi depressioni respiratorie."
    ),
    BestPracticeItem(
        "Non metterti alla guida, anche se ti senti \"lucido\"",
        "L'alcol compromette la visione periferica, rallenta drasticamente i riflessi e altera la percezione delle distanze ben prima che tu ti senta fisicamente ubriaco. Affidati esclusivamente al timer di recupero e non alle tue sensazioni del momento."
    )
)

/** Sezione "Bevi in sicurezza" (tab Recupero): due liste espandibili, cosa fare e cosa evitare. */
@Composable
fun BestPracticesSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Bevi in sicurezza", style = MaterialTheme.typography.headlineSmall)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("✅ Cosa fare", style = MaterialTheme.typography.titleMedium)
            COSA_FARE.forEach { item -> FaqItem(item) }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("❌ Cosa non fare", style = MaterialTheme.typography.titleMedium)
            COSA_NON_FARE.forEach { item -> FaqItem(item) }
        }
    }
}

@Composable
private fun FaqItem(item: BestPracticeItem) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "faqArrowRotation")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.clickable { expanded = !expanded }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Comprimi" else "Espandi",
                    modifier = Modifier.rotate(arrowRotation)
                )
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                )
            }
        }
    }
}
