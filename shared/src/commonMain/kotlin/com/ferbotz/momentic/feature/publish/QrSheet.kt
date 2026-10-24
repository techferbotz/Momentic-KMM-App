package com.ferbotz.momentic.feature.publish

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.designsystem.components.InlineBanner
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.MomenticBottomSheet
import com.ferbotz.momentic.designsystem.components.MomenticIconButton
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.designsystem.qr.QrCode
import com.ferbotz.momentic.designsystem.qr.QrCodeImage
import com.ferbotz.momentic.designsystem.qr.QrErrorCorrection
import com.ferbotz.momentic.designsystem.qr.QrMark
import com.ferbotz.momentic.designsystem.qr.qrBitmap
import com.ferbotz.momentic.platform.rememberImageSaver
import com.ferbotz.momentic.platform.rememberShareSheet
import com.ferbotz.momentic.platform.shareText
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.cd_close
import com.ferbotz.momentic.resources.qr_image_description
import com.ferbotz.momentic.resources.qr_not_saved
import com.ferbotz.momentic.resources.qr_save
import com.ferbotz.momentic.resources.qr_saved
import com.ferbotz.momentic.resources.qr_share
import com.ferbotz.momentic.resources.qr_subtitle
import com.ferbotz.momentic.resources.qr_subtitle_plain
import com.ferbotz.momentic.resources.qr_title
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.literal
import org.jetbrains.compose.resources.stringResource

/** The saved image: big enough to print, the code with its quiet zone and the mark. */
private const val SAVED_SIZE_PX = 1200

/**
 * A moment's QR code in a sheet — for a printed card, a screen at the venue. Any phone camera opens
 * [url]. "Save image" puts the code in the photo library; "Share" sends the link with [shareLine].
 */
@Composable
fun QrSheet(url: String, momentTitle: String?, shareLine: String, onDismiss: () -> Unit) {
    val c = Momentic.colors
    val code = remember(url) { QrCode.encode(url, QrErrorCorrection.Quartile) }
    val saver = rememberImageSaver()
    val share = rememberShareSheet()
    // Whether the last save worked, said inside the sheet: a snackbar would sit behind it.
    var saveResult by remember { mutableStateOf<Boolean?>(null) }
    val measurer = rememberTextMeasurer()
    val mark = QrMark(
        letter = "m",
        ink = c.white,
        circle = c.roseDeep,
        tile = Color.White,
        measurer = measurer,
        style = Momentic.type.quote.copy(fontWeight = FontWeight.SemiBold),
    )
    val address = url.removePrefix("https://").removePrefix("http://")
    val saved = stringResource(Res.string.qr_saved)
    val notSaved = stringResource(Res.string.qr_not_saved)

    MomenticBottomSheet(onDismissRequest = onDismiss) {
        val close = dismiss
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xxs)) {
                Text(stringResource(Res.string.qr_title), style = Momentic.type.h1, color = c.ink)
                Text(
                    text = momentTitle?.let { stringResource(Res.string.qr_subtitle, it) } ?: stringResource(Res.string.qr_subtitle_plain),
                    style = Momentic.type.bodySmall,
                    color = c.inkSoft,
                )
            }
            MomenticIconButton(MomenticIcons.Close, stringResource(Res.string.cd_close), onClick = close)
        }
        Column(Modifier.fillMaxWidth().padding(top = MomenticSpacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            if (code != null) {
                Box(
                    Modifier
                        .clip(MomenticShapes.card)
                        .background(Color.White)
                        .border(1.dp, c.line, MomenticShapes.card)
                        .padding(MomenticSpacing.md),
                ) {
                    QrCodeImage(code, stringResource(Res.string.qr_image_description, address), Modifier.size(216.dp), dark = c.ink, mark = mark)
                }
            }
            Text(
                text = address,
                style = Momentic.type.label.literal(),
                color = c.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = MomenticSpacing.sm),
            )
        }
        saveResult?.let { ok ->
            InlineBanner(
                text = if (ok) saved else notSaved,
                tone = if (ok) MessageTone.Success else MessageTone.Error,
                modifier = Modifier.padding(top = MomenticSpacing.md),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = MomenticSpacing.lg), horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
            SecondaryButton(
                text = stringResource(Res.string.qr_save),
                onClick = {
                    val image = code?.let { qrBitmap(it, SAVED_SIZE_PX, c.ink, Color.White, mark) } ?: return@SecondaryButton
                    saveResult = null
                    saver.save(image, "momentic-${address.substringAfterLast('/')}") { ok -> saveResult = ok }
                },
                icon = MomenticIcons.Download,
                enabled = code != null,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = stringResource(Res.string.qr_share),
                onClick = { share.share(shareText(shareLine, url)) },
                icon = MomenticIcons.Share,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
