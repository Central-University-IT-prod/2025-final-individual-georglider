package ru.georglider.prod.payload.request.advertiser.campaign

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import ru.georglider.prod.payload.dto.advertiser.campaign.Targeting

data class CreateCampaignRequest (
    @field:NotNull @field:Min(0, message = "Количество просмотров не может быть отрицательным") @field:JsonProperty("impressions_limit") val impressionsLimit: Int? = null,
    @field:NotNull @field:Min(0, message = "Количество кликов не может быть отрицательным") @field:JsonProperty("clicks_limit") val clicksLimit: Int? = null,
    @field:NotNull @field:Min(0, message = "Цена не может быть отрицательной") @field:JsonProperty("cost_per_impression") val costPerImpression: Double? = null,
    @field:NotNull @field:Min(0, message = "Цена не может быть отрицательной") @field:JsonProperty("cost_per_click") val costPerClick: Double? = null,
    @field:NotNull @field:JsonProperty("ad_title")
    @field:NotBlank(message = "Поле название не может быть пустым")
    @field:Size(max = 255, message = "Поле название не может превышать 255 символов")
    val adTitle: String? = null,
    @field:NotNull @field:JsonProperty("ad_text") val adText: String? = null,
    @field:NotNull @field:JsonProperty("start_date") val startDate: Int? = null,
    @field:NotNull @field:JsonProperty("end_date") val endDate: Int? = null,
    @field:JsonProperty("targeting") val targeting: Targeting? = null
) : CampaignRequest {

    fun isValid(): Boolean {
        val vTarget = targeting == null || targeting.isValid()
        val vDate = (startDate  ?: Int.MIN_VALUE) <= (endDate ?: Int.MAX_VALUE)
        val vClickImpression = clicksLimit!! <= impressionsLimit!!
        return vTarget && vDate && vClickImpression
    }

    override fun getIImpressionsLimit(): Int? = impressionsLimit
    override fun getIClicksLimit(): Int? = clicksLimit
    override fun getICostPerImpression(): Double? = costPerImpression
    override fun getICostPerClick(): Double? = costPerClick
    override fun getIAdTitle(): String? = adTitle
    override fun getIAdText(): String? = adText
    override fun getIStartDate(): Int? = startDate
    override fun getIEndDate(): Int? = endDate
    override fun getITargeting(): Targeting? = targeting

}