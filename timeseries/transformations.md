Yes. Given that you want the transformations primarily as **controlled perturbations of individual signal properties**, I would expand M1 beyond generic perturbations and deliberately include transformations that target different properties.

Your current set already covers:

* `AddAverage` → level/mean
* `AddNoise` → variance, volatility, distribution, spectral content
* `Smoothing` → volatility, autocorrelation, high-frequency content
* `AddSineWave` → frequency-domain properties

I would consider the following.

| Transformation        | Main property targeted                | Expected effect                         |
| --------------------- | ------------------------------------- | --------------------------------------- |
| `AddTrend`            | linear trend                          | controlled change in trend              |
| `Scale`               | amplitude, standard deviation, range  | multiplicative change                   |
| `Invert`              | mean/trend/skewness                   | sign reversal                           |
| `Difference`          | volatility, autocorrelation, trend    | removes level/trend, emphasizes changes |
| `RemoveTrend`         | linear trend                          | trend → ~0                              |
| `AddSeasonality`      | seasonal structure, frequency         | introduces periodic component           |
| `ChangeFrequency`     | dominant frequency, spectral centroid | shifts periodic component               |
| `LowPassFilter`       | spectral centroid, volatility         | removes high frequencies                |
| `HighPassFilter`      | spectral centroid, volatility         | removes low frequencies                 |
| `OutlierInjection`    | kurtosis, skewness, amplitude         | increases tail behavior                 |
| `OutlierClipping`     | kurtosis, amplitude                   | decreases extreme values                |
| `QuantileTransform`   | skewness, kurtosis                    | changes distribution shape              |
| `TimeReversal`        | autocorrelation, trend                | reverses temporal direction             |
| `TimeWarping`         | frequency/temporal structure          | changes temporal scale                  |
| `BlockShuffle`        | autocorrelation, spectral structure   | destroys local temporal dependence      |
| `AmplitudeModulation` | amplitude/variance                    | time-varying amplitude                  |

A few of these are particularly useful for your experiment.

### 1. AddTrend [ADDED]

This is probably the most obvious missing transformation.

For example:

```java
@AddTrend:
    timeseries.addLinearTrend(
        forecastrequest.context,
        0.01
    )
```

Conceptually:

$$
T(x_t)=x_t+\alpha t
$$

This gives you a transformation where you know exactly how the `linearTrend` property should change.

You can vary \(\alpha\), making this a very clean transformation-strength experiment.

### 2. Scale [ADDED]

```java
@Scale:
    arrays.forEach(
        forecastrequest.context,
        ARRAY_ELEMENT * 1.2
    )
```

This is useful because it gives you a clean test of amplitude-related properties:

* standard deviation
* range
* percentile range
* average absolute derivative

For example,

$$
T(x)=cx.
$$

Then, ignoring numerical issues:

$$
\sigma(T(x))=|c|\sigma(x)
$$

and

$$
\text{range}(T(x))=|c|\text{range}(x).
$$

This could be particularly interesting for determining whether a model's forecast scales consistently with its input.

### 3. AddSeasonality / periodic component [NOT NOW]

Your sine-wave idea is strong. I would make it slightly more general than just one frequency:

$$
T(x_t)=x_t+A\sin(2\pi f t).
$$

You can independently vary:

* amplitude \(A\)
* frequency \(f\)

This lets you study whether models respond differently to **weak vs. strong periodic signals** and **low vs. high frequencies**.

For your properties:

* `dominantFrequency`
* `spectralCentroid`
* spectral entropy, if you add it later

are directly relevant.

A useful extension would be adding **two frequencies**:

$$
T(x_t)=x_t+A_1\sin(2\pi f_1t)+A_2\sin(2\pi f_2t).
$$

That tests whether the model preserves multiple frequency components rather than simply responding to the strongest one.

### 4. Outlier injection [ADDED]

This would be very useful for your distributional properties.

For example:

```text
@AddOutliers
    randomly select k points
    x[t] += α * standardDeviation(x)
```

Primarily targets:

* kurtosis
* skewness
* max/min
* percentile range
* standard deviation

You could distinguish between:

```text
positive outliers
negative outliers
symmetric outliers
```

Positive-only outliers should primarily affect skewness, while symmetric extreme values should primarily affect kurtosis.

That gives you a particularly nice controlled experiment.

### 5. Outlier clipping [ADDED]

The inverse experiment is also useful:

$$
T(x_t)=\min(\max(x_t,L),U)
$$

where \(L,U\) are chosen quantiles.

This should primarily modify:

* kurtosis
* amplitude/range
* standard deviation
* potentially skewness

It also lets you investigate whether models respond differently to **removing extreme observations** versus adding them.

### 6. Differencing [NOT NOW]

First-order differencing:

$$
T(x_t)=x_t-x_{t-1}.
$$

This is interesting because it fundamentally changes the temporal structure rather than simply perturbing the values.

It should affect:

* linear trend
* autocorrelation
* volatility
* spectral content

It also gives you an interesting question: does the forecasting model respond appropriately when the underlying representation changes from **levels to changes**?

However, I would classify this separately from your simpler perturbations because it changes the semantics of the signal considerably.

### 7. Time reversal [ADDED]

This is an interesting one for your autocorrelation/trend analysis:

$$
T(x_1,x_2,\ldots,x_n)
=
(x_n,x_{n-1},\ldots,x_1).
$$

Autocorrelation should theoretically remain largely unchanged under reversal, whereas linear trend changes sign.

So this gives you a transformation with a very specific expected property behavior:

$$
\rho_1(T(x))\approx\rho_1(x)
$$

but

$$
\text{trend}(T(x))\approx-\text{trend}(x).
$$

That makes it potentially very useful for separating properties that should be invariant from properties that should change.

### 8. Low-pass / high-pass filtering

These are probably the most natural additions if you want to investigate frequency properties seriously.

Low-pass:

$$
T_{\mathrm{low}}(x)=\text{LowPass}(x)
$$

should generally reduce high-frequency content.

High-pass:

$$
T_{\mathrm{high}}(x)=\text{HighPass}(x)
$$

should remove low-frequency components.

These give you much more controlled frequency perturbations than Gaussian noise.

For example:

```text
Smoothing
    ↓
high-frequency attenuation

LowPassFilter
    ↓
explicit high-frequency removal

HighPassFilter
    ↓
explicit low-frequency removal

AddSineWave
    ↓
explicit frequency injection
```

Together, these could form a coherent **frequency-domain transformation family**.

### 9. Block shuffle [NOT NOW]

Randomly shuffle observations within local blocks.

For example, divide the series into blocks of length \(k\), then shuffle the values inside each block.

This primarily targets temporal dependence:

* autocorrelation ↓
* local volatility may remain approximately similar
* distributional properties remain approximately unchanged

That is valuable because it gives you a transformation where you deliberately change **temporal structure while preserving the marginal distribution**.

This is exactly the kind of transformation that can distinguish whether a model reacts to temporal structure rather than simply the distribution of values.

### 10. Time warping / resampling [NOT NOW]

Stretch or compress the time axis:

$$
T(x_t)=x_{\alpha t}.
$$

This changes temporal/frequency characteristics without necessarily changing the value distribution.

It could affect:

* dominant frequency
* spectral centroid
* autocorrelation
* trend measured per observation

This is more complex experimentally, though, because interpolation/resampling introduces implementation effects. I would probably leave it for a later experiment.

---

For your current study, I would prioritize the transformations into four families:

```text
Level / scale
    AddAverage
    Scale
    AddTrend

Distribution
    AddNoise
    AddOutliers
    ClipOutliers
    QuantileTransform

Temporal structure
    Smoothing
    Differencing
    TimeReversal
    BlockShuffle

Frequency structure
    AddSineWave
    LowPassFilter
    HighPassFilter
    ChangeFrequency
```

The important methodological point is that **you don't necessarily want every transformation to affect every property**. In fact, it is useful if some transformations are designed to be relatively selective.

For example:

> `BlockShuffle` changes autocorrelation while approximately preserving the marginal distribution.

> `AddOutliers` changes kurtosis/skewness while leaving the temporal ordering mostly intact.

> `AddSineWave` introduces a controlled frequency component.

> `AddTrend` changes the trend by a controlled amount.

> `Scale` changes amplitude/variance while preserving normalized distributional shape.

That gives you a much stronger experimental design than simply throwing generic noise/smoothing transformations at the models. It lets you ask whether a model's forecast responds to the **specific signal property that was deliberately manipulated**.
