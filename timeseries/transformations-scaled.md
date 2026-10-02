Yes. I would strongly recommend treating transformation strength as an experimental factor rather than having one arbitrary strength per transformation.

The important part is to choose strengths that represent **meaningfully different perturbation regimes**, rather than simply multiplying parameters by 1, 2, 3, 4, etc.

A good general structure would be three levels:

* **Weak**: clearly measurable, but relatively small perturbation
* **Moderate**: substantial but still plausible
* **Strong**: deliberately substantial stress test

I would avoid going much beyond three or four levels initially.

### AddAverage

Your current transformation is essentially

$$
T(x_t)=x_t+\alpha
$$

where you currently use

$$
\alpha=\bar{x}.
$$

That is actually a very large and somewhat arbitrary choice because the appropriate magnitude depends on the series.

I'd make it relative to the series' standard deviation instead:

$$
T(x_t)=x_t+k\sigma_x.
$$

For example:

| Strength | \(k\) |
| -------- | ----: |
| Weak     |   0.5 |
| Moderate |   1.0 |
| Strong   |   2.0 |

So:

```text
AddAverage(0.5 * std)
AddAverage(1.0 * std)
AddAverage(2.0 * std)
```

This has a nice interpretation: the entire signal is shifted by half, one, or two standard deviations.

However, I'd rename this transformation to something like `AddOffset`, since it isn't really adding the average in a conceptual sense.

---

### Scale

For

$$
T(x_t)=kx_t
$$

I'd use multiplicative factors that represent clearly different changes:

| Strength | \(k\) | Change |
| -------- | ----: | -----: |
| Weak     |   1.1 |   +10% |
| Moderate |   1.5 |   +50% |
| Strong   |   2.0 |  +100% |

So:

```text
Scale(1.1)
Scale(1.5)
Scale(2.0)
```

You could also test reductions:

```text
Scale(0.9)
Scale(0.5)
Scale(0.25)
```

But I wouldn't automatically include both directions in the first experiment. There is an important distinction here: `Scale(2)` and `Scale(0.5)` are not equivalent for most forecasting models because they change the signal in opposite directions relative to its original magnitude.

A particularly clean design would be to test both **amplification and attenuation** at corresponding strengths:

$$
0.5,\;1,\;2.
$$

That gives you symmetry around the original signal on a log scale.

---

### AddNoise

This is one where your current definition is already quite sensible:

$$
\sigma_{\text{noise}}=0.1\sigma_x.
$$

I'd expand it to something like:

| Strength    |         Noise SD |
| ----------- | ---------------: |
| Weak        | \(0.05\sigma_x\) |
| Moderate    | \(0.10\sigma_x\) |
| Strong      | \(0.25\sigma_x\) |
| Very strong | \(0.50\sigma_x\) |

So:

```text
AddNoise(0.05 * std)
AddNoise(0.10 * std)
AddNoise(0.25 * std)
AddNoise(0.50 * std)
```

I think 0.05/0.10/0.25/0.50 is more useful than 0.1/0.2/0.3/0.4 because the former represents qualitatively different noise regimes.

I'd probably use only the first three for the main experiment and reserve `0.5σ` as a stress-test condition.

One important point: because Gaussian noise has expected variance \(\sigma_n^2\), adding noise with \(0.5\sigma_x\) does not simply mean "50% more standard deviation." The resulting standard deviation is approximately

$$
\sigma_{T(x)}
=
\sqrt{\sigma_x^2+\sigma_n^2}.
$$

So with \(0.1\sigma_x\), the resulting SD is only about \(1.005\sigma_x\). With \(0.5\sigma_x\), it becomes about \(1.118\sigma_x\).

That means your `AddNoise(0.1 * std)` is actually a **very weak perturbation in terms of overall variance**, even though the individual observations receive noise with SD equal to 10% of the original SD.

This is worth considering when choosing strengths.

---

### Smoothing

This one should be parameterized by the window size.

Your current:

```text
round(0.05 * length)
```

is reasonable, but I'd use several relative window sizes, e.g.:

| Strength |         Window |
| -------- | -------------: |
| Weak     | 2.5% of series |
| Moderate |             5% |
| Strong   |            10% |

So:

```text
Smoothing(0.025 * n)
Smoothing(0.05 * n)
Smoothing(0.10 * n)
```

with sensible minimum/odd-window handling.

But there is an important caveat: a 5% window does **not** represent the same smoothing strength for all time series. If one series has 100 observations and another has 10,000, the resulting temporal filtering is very different.

For your experiment, I would therefore record the **actual smoothing window and the resulting change in measurable properties**, rather than assuming that `5%` means a standardized perturbation.

---

### AddTrend

This is another transformation where multiple strengths make a lot of sense.

You can define:

$$
T(x_t)=x_t+k\sigma_x\frac{t-\bar t}{n}.
$$

Then \(k\) controls the total trend magnitude relative to the signal scale.

For example:

```text
AddTrend(0.25 * std)
AddTrend(0.50 * std)
AddTrend(1.00 * std)
```

where the trend produces roughly 0.25, 0.5, or 1 standard deviation of change across the series.

This is much more interpretable than specifying an absolute slope.

---

### AddSineWave

Here I'd actually separate **amplitude** and **frequency**.

For amplitude:

```text
0.25 * std
0.50 * std
1.00 * std
```

For frequency, choose frequencies relative to the available sampling frequency / series length rather than arbitrary values.

For example, specify the period:

```text
period = 0.05 n
period = 0.10 n
period = 0.25 n
```

or explicitly select low-, medium-, and high-frequency components.

This lets you ask two different questions:

1. How does the model respond to increasingly strong periodic structure?
2. How does the model respond to periodic structure at different frequencies?

Those should probably be separate experimental factors.

---

### Outliers

If you add this transformation, strength could control either magnitude or number of outliers.

I'd use magnitude first:

```text
1σ
2σ
4σ
```

with perhaps 1%, 2%, and 5% of observations affected as a separate factor later.

That gives you a clean distinction between:

* **outlier magnitude**
* **outlier prevalence**

---

### One thing I would change in the experimental design

I wouldn't make every transformation have exactly the same number of arbitrary strength values.

Instead, define a transformation-specific parameterization, but aim for approximately three **effect regimes**:

```text
weak
moderate
strong
```

Then measure the actual resulting property change.

For example:

```text
Transformation      Parameter       Weak       Moderate    Strong

AddOffset           k·σ             0.5σ       1σ          2σ
Scale               factor          1.1        1.5         2.0
AddNoise            σ_noise         0.05σ      0.10σ       0.25σ
Smoothing           window          2.5%       5%          10%
AddTrend            total change    0.25σ      0.5σ        1σ
AddSineWave         amplitude       0.25σ      0.5σ        1σ
AddOutliers         magnitude       1σ         2σ          4σ
```

The key is that these numbers are **initial parameter choices, not the final definition of transformation strength**.

For your analysis, I would actually store both:

$$
\text{nominal strength}
$$

and

$$
\Delta P=P(T(x))-P(x).
$$

The second one is particularly important for your project. For example, two transformations might both be labelled "moderate", but one might change standard deviation by 2% and another by 40%. The observed property change gives you a much more meaningful common scale for comparing transformations.

That also connects directly to your proposed sensitivity analysis:

$$
S_P(x,T(x)) =
P(T(x))-P(x).
$$

You can then investigate whether the forecast's property change

$$
S_P(f(x),f(T(x)))
$$

grows systematically with the **actual induced change** \(S_P(x,T(x))\), rather than merely with an arbitrary transformation parameter. This is probably the cleanest way to make the different transformation families comparable.
