Migrated from GitHub issue 7 by sfischer

## Structural / property-based metamorphic relations

Instead of requiring the transformed forecast to remain numerically close to the original forecast, some MRs can define expected changes in structural forecast properties.

This avoids explicit deviation thresholds and focuses on directional or monotonic behavioral expectations.

---

### 1. Noise injection MR

Given:

- $x$: original input series
- $T_{\text{noise}}(x)$: input series with added noise
- $f(x)$: original forecast
- $f(T_{\text{noise}}(x))$: forecast after noise injection

Define forecast smoothness using first-order differences:

$$
D(y) =
\frac{1}{H-1}
\sum_{t=1}^{H-1}
|y_{t+1} - y_t|
$$

- $D(y)$: average absolute derivative magnitude of forecast series $y$

Expected relation:

$$
D(f(T_{\text{noise}}(x))) \ge D(f(x))
$$

Interpretation:

- adding noise to the input should increase short-term variability of the forecast

---

### 2. Smoothing MR

Given:

- $T_{\text{smooth}}(x)$: smoothed version of input series

Expected relation:

$$
D(f(T_{\text{smooth}}(x))) \le D(f(x))
$$

Interpretation:

- smoothing the input should reduce forecast variability

---

### 3. Volatility-preservation MR

Define forecast volatility:

$$
V(y) = \text{Std}(y)
$$

Expected relation under noise injection:

$$
V(f(T_{\text{noise}}(x))) \ge V(f(x))
$$

Expected relation under smoothing:

$$
V(f(T_{\text{smooth}}(x))) \le V(f(x))
$$

- $\text{Std}(y)$: standard deviation of forecast series $y$

---

### 4. Trend-preservation MR

Given forecast series:

$$
y = [y_1, \dots, y_H]
$$

fit a linear regression line:

$$
y_t \approx a t + b
$$

where:

- $a$: slope of the fitted trend line
- $b$: intercept
- $t \in \{1, \dots, H\}$: forecast horizon index

Define:

$$
\text{Trend}(y) = a
$$

Expected relation:

$$
\text{sign}(\text{Trend}(f(x)))=\text{sign}(\text{Trend}(f(T(x))))
$$

- $\text{sign}(\cdot)$: sign function indicating positive, negative, or zero trend direction

Interpretation:

- transformations such as smoothing or moderate noise should not invert the overall forecast trend direction
- the transformed forecast may differ locally, but should preserve the dominant directional tendency


### 5. Frequency-domain energy

Analyze spectral characteristics using Fourier transform.

Define:

$$
\text{Energy}_{\text{high}}(y)=\sum_{k > k_c}|\hat{y}_k|^2
$$

- $\hat{y}_k$: Fourier coefficient at frequency $k$
- $k_c$: cutoff frequency

Expected MR examples:

- smoothing should reduce high-frequency energy
- noise injection should increase high-frequency energy

Interpretation:

- captures fine-grained fluctuations and signal roughness

---

### 6. Forecast bias / level shift

Measure average forecast level.

Define:

$$
\text{Mean}(y)=\frac{1}{H}\sum_{t=1}^{H} y_t
$$

Expected MR examples:

- additive transformations should shift mean predictably
- scaling transformations should scale mean proportionally

Interpretation:

- verifies consistency of forecast level behavior


---

### 7. Range / amplitude

Measure spread between minimum and maximum forecast values.

Define:

$$
\text{Range}(y)=\max_t y_t - \min_t y_t
$$

Expected MR examples:

- smoothing should reduce range
- scaling should proportionally change range

Interpretation:

- captures forecast amplitude changes


---

## Key conceptual difference

Traditional MR formulation:

$$
f(T(x)) \approx T(f(x))
$$

Property-based formulation:

$$
P(f(T(x))) \;\; \relates \;\; P(f(x))
$$

where:

- $P(\cdot)$ extracts a structural property of the forecast
- $\relates$ denotes an expected directional relation (e.g., increase, decrease, preservation)

This shifts MR testing from approximate numerical equality toward behavioral consistency constraints.



---

## Additional candidate properties

### 8. Forecast uncertainty

For probabilistic forecasting models, the model output is not only a point forecast but a predictive distribution. A useful property is the width of the prediction interval.

Given quantile forecasts:

$$
q_{\alpha_1}(y), ..., q_{\alpha_n}(y)
$$

define uncertainty as:

$$
U(y)=\frac{1}{H}\sum_{t=1}^{H}
(q_{\alpha_{high},t}-q_{\alpha_{low},t})
$$

where:

- $q_{\alpha_{high}}$: upper prediction quantile (e.g., 0.9)
- $q_{\alpha_{low}}$: lower prediction quantile (e.g., 0.1)

Expected MR examples:

- adding noise to the input should increase forecast uncertainty
- removing context should increase forecast uncertainty
- providing cleaner or more informative context should reduce uncertainty

Interpretation:

- tests whether the model's uncertainty estimates react appropriately to changes in input quality and information availability

---

### 9. Autocorrelation / temporal dependency

Forecasts can be evaluated based on whether they preserve temporal dependencies.

Define lag- $k$ autocorrelation:

$$
\rho(k)=
\frac{\sum_t(y_t-\bar{y})(y_{t-k}-\bar{y})}
{\sum_t(y_t-\bar{y})^2}
$$

Expected MR examples:

- transformations that increase temporal structure should increase autocorrelation
- transformations that destroy temporal dependencies (e.g., shuffling parts of the input) should reduce autocorrelation in the forecast

Interpretation:

- captures whether the model responds appropriately to temporal structure rather than only matching point-wise statistics

---

### 10. Spectral peak / seasonality strength

Instead of only measuring total frequency energy, explicitly measure whether dominant periodic patterns are preserved.

Define the dominant frequency:

$$
f^*=\arg\max_k |\hat{y}_k|^2
$$

or spectral energy around a known seasonal frequency:

$$
E_f(y)=\sum_{k \in F}|\hat{y}_k|^2
$$

Expected MR examples:

- increasing the amplitude of a known seasonal component in the input should increase the corresponding spectral energy in the forecast
- removing seasonal structure should reduce the corresponding spectral peak

Interpretation:

- tests whether the model captures and propagates meaningful periodic patterns

---

### 11. Context dependency / information sensitivity

Forecasting models should react differently depending on how much relevant historical information is available.

Possible transformations:

- remove parts of the historical context
- shorten the context window
- mask recent observations

Possible output properties:

- forecast uncertainty
- forecast error (if ground truth is available)
- deviation from the full-context forecast

Expected MR examples:

- removing relevant context should not improve forecasting error consistently
- removing context should generally increase uncertainty

Interpretation:

- tests whether the model appropriately uses available historical information

---

### 12. Forecast error sensitivity

Some MRs can evaluate not only forecast properties but changes in predictive performance under controlled transformations.

Given ground truth $y$ and forecast $\hat{y}$:

$$
E(y,\hat{y}) = RMSE(y,\hat{y})
$$

Expected MR examples:

- removing important context should not systematically improve RMSE
- adding informative transformations should not degrade forecasting performance

Interpretation:

- directly connects metamorphic behavior with forecasting quality while avoiding reliance on absolute error thresholds

---

### 13. Distributional shape properties

Beyond mean and variance, the distribution of forecast values can be characterized using higher-order statistics.

Possible properties:

- skewness
- kurtosis
- entropy

Expected MR examples:

- transformations that introduce asymmetry should influence forecast skewness
- increased randomness should increase forecast entropy

Interpretation:

- captures changes in forecast distribution that are not reflected by mean and variance alone