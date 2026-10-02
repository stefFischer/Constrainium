Yes. Looking at your current properties as a set, you already cover a lot:

* Level: mean
* Dispersion/amplitude: standard deviation, range, percentile range
* Local dynamics: average absolute derivative
* Trend: linear trend
* Temporal dependence: lag-1 autocorrelation
* Distribution shape: skewness, kurtosis
* Frequency location: dominant frequency, spectral centroid

So I would avoid adding things like variance, IQR, MAD, RMS, or coefficient of variation: those mostly give you another measure of properties you already have.

There are, however, several genuinely different dimensions I think are worth considering.

### 1. Spectral entropy — genuinely new [ADDED]

You currently measure *where* the spectral energy is concentrated, but not *how distributed* it is.

Given normalized spectral power \(p_k\),

$$
P_{\text{specEntropy}}(x)
=
-\sum_k p_k\log p_k.
$$

Low entropy → energy concentrated in a few frequencies.

High entropy → energy distributed across many frequencies.

This is substantially different from dominant frequency and spectral centroid.

It would be particularly useful with:

* `AddNoise`
* `Smoothing`
* `AddSineWave`
* `LowPassFilter`

For example, adding a single strong sine wave can make the spectrum more concentrated, while noise tends to distribute energy more broadly.

**I would definitely consider adding this.**

---

### 2. Seasonality strength — genuinely new [NOT NOW]

Autocorrelation can indicate periodicity, but it does not explicitly measure the strength of a seasonal component.

If the data has a known period \(s\), you could decompose it into trend/seasonal/residual components and define something like

$$
P_{\text{seasonal}} =
1-\frac{\operatorname{Var}(R)}
{\operatorname{Var}(S+R)}
$$

where \(S\) is the seasonal component and \(R\) the remainder.

This lets you test transformations such as:

```text
AddSeasonality
RemoveSeasonality
ChangeSeasonalityAmplitude
```

and ask whether the forecast preserves the induced change in seasonal structure.

This is genuinely different from your current properties, although it requires knowing or estimating a seasonal period.

---

### 3. Zero-crossing rate — genuinely new [ADDED as LevelCrossingRate]

Measure how often the signal crosses a reference level, usually zero or the mean:

$$
P_{\text{ZCR}}(x)
=
\frac{1}{n-1}
\sum_{t=2}^{n}
\mathbf{1}
[(x_t-c)(x_{t-1}-c)<0].
$$

You can use \(c=\bar{x}\) to make it translation-invariant.

It captures **oscillation frequency** in a way that is quite different from spectral centroid.

For example:

* adding high-frequency oscillations → ZCR increases
* smoothing → ZCR decreases
* low-pass filtering → ZCR decreases

It is simple, scalar, and parameter-light.

I think this is a good candidate.

---

### 4. Sample entropy / approximate entropy — genuinely new [NOT NOW]

This moves beyond linear temporal dependence.

Your lag-1 autocorrelation asks:

> How linearly dependent are neighboring observations?

Sample entropy asks roughly:

> How predictable/repetitive is the local pattern structure?

For example:

* highly regular periodic signal → low entropy
* noisy/irregular signal → high entropy

This could expose behavior that autocorrelation completely misses.

The downside is that sample entropy requires parameters such as embedding dimension \(m\) and tolerance \(r\). So it is less attractive for your goal of simple, data-agnostic metrics.

I would consider it a **second-stage property**, not one of the initial core metrics.

---

### 5. Hurst exponent / long-range dependence — potentially very interesting [NOT NOW]

Lag-1 autocorrelation only captures very short-term dependence.

The Hurst exponent \(H\) attempts to characterize longer-range temporal dependence:

* \(H>0.5\): persistent behavior
* \(H\approx0.5\): approximately uncorrelated/random-walk-like scaling
* \(H<0.5\): anti-persistent behavior

This gives you a genuinely different temporal property.

A transformation that destroys long-range dependence could then be tested against the forecast.

The drawback is that Hurst estimation can be unreliable on short series and depends on the estimator.

So again, interesting scientifically, but I wouldn't make it a first-line metric.

---

### 6. Stationarity — potentially important, but not a single simple property [NOT NOW]

This is actually a major dimension missing from your current set.

You could investigate whether a transformation changes:

* mean stability
* variance stability
* temporal dependence

However, "stationarity" itself is not naturally a scalar property like your others. Statistical tests such as ADF/KPSS produce test statistics or p-values, but those are not as clean for your sensitivity framework.

I would therefore **not add stationarity as a property yet**, unless you specifically want to study it.

---

### 7. Turning-point rate — genuinely different from volatility [ADDED]

Define the proportion of observations where the direction changes:

$$
P_{\text{turn}}(x)
=
\frac{1}{n-2}
\sum_{t=2}^{n-1}
\mathbf{1}
[
(x_t-x_{t-1})(x_{t+1}-x_t)<0
].
$$

This measures how frequently the series changes direction.

It differs from your average absolute derivative:

```text
averageAbsoluteDerivative
    → how much the signal moves

turningPointRate
    → how frequently the direction changes
```

A smooth oscillation and a noisy series can have similar average absolute changes but very different turning-point rates.

This could be particularly useful for:

* smoothing
* noise injection
* high-frequency sine waves

I think this is another genuinely useful candidate.

---

### 8. Quantile-based asymmetry — probably redundant

You could measure things like upper/lower tail spread or interquantile asymmetry, but given that you already have skewness, kurtosis, and percentile range, I would not prioritize these.

Similarly, I would not add:

* variance → redundant with standard deviation
* MAD → another dispersion measure
* RMS → largely amplitude/scale
* IQR → another range/dispersion measure
* coefficient of variation → normalized dispersion
* mean absolute change vs standard deviation of differences → closely related to your volatility measure

---

### My shortlist

If the goal is **maximum new information with minimum redundancy**, I would add:

| Property                 | What new dimension it captures            |
| ------------------------ | ----------------------------------------- |
| **Spectral entropy**     | Distribution of energy across frequencies |
| **Seasonality strength** | Explicit periodic/seasonal structure      |
| **Zero-crossing rate**   | Oscillation rate                          |
| **Turning-point rate**   | Directional complexity                    |
| **Hurst exponent**       | Long-range dependence                     |
| **Sample entropy**       | Nonlinear/local temporal complexity       |

Of these, I would start with **spectral entropy, zero-crossing rate, and turning-point rate**. They are scalar, interpretable, relatively easy to compute, and map naturally onto transformations you are already considering.

The resulting property space would then cover fairly distinct dimensions:

$$
\boxed{
\begin{array}{ll}
\text{Level} & \text{mean}\\
\text{Scale} & \text{std, range}\\
\text{Local movement} & \text{average absolute derivative}\\
\text{Trend} & \text{linear slope}\\
\text{Short-term dependence} & \text{ACF}_1\\
\text{Distribution shape} & \text{skewness, kurtosis}\\
\text{Frequency location} & \text{dominant frequency, spectral centroid}\\
\text{Frequency distribution} & \text{spectral entropy}\\
\text{Oscillation} & \text{zero-crossing rate}\\
\text{Directional complexity} & \text{turning-point rate}\\
\text{Seasonality} & \text{seasonal strength}\\
\text{Long-range dependence} & \text{Hurst exponent}\\
\text{Nonlinear complexity} & \text{sample entropy}
\end{array}
}
$$

That is a much more defensible property set than adding another five or ten closely related measures of scale, variance, or local volatility.
