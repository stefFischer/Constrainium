# Time-Series Metamorphic Relations (MRs)

## Notation Legend for MR-TS Definitions

This legend summarizes the mathematical symbols and operators used across the MR-TS specifications.

---

### Core Functions and Variables

- $x$  
  Original input instance (e.g., time series, feature vector, sequence, image).

- $x'$  
  Transformed input after applying an input transformation.

- $T(x)$  
  Input transformation operator applied to $x$.

- $f(x)$  
  Model output for input $x$.  

- $f(x')$  
  Model output for the transformed input.


### Task-Specific Output Interpretation

- **Forecasting:**  
  - $f_H(x)$  
    Forecast of horizon length $H$.
  - $f(x)(t)$  
    Forecast value at horizon step $t$.
  - $f_{H+K}(x)[1:H]$  
    First $H$ forecast steps of a longer forecast.
  - $t$ Forecast step index (e.g., $t = 1, \dots, H$).
  - $T$ Last observed time step in the input series.

- **Classification:**  
  - $f(x)$ — predicted class label  
  - $f(x) \in \mathcal{Y}$ — label space  
  - $f(x)_k$ — predicted probability or score for class $k$

---


## A) Forecasting MRs (1–25)

### MR-TS-01 — Additive Offset Equivariance

- **Input transformation:**  
  $x'(t) = x(t) + k$

- **Validation check:**  
  $f(x')(t) \approx f(x)(t) + k \quad \text{for all } t \text{ in forecast horizon}$

- **Pass/Fail criteria:**  
  Pass if $|f(x')(t) - (f(x)(t) + k)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model outputs forecasts in the same units/scale as the input (i.e., not always normalized).

- **TiRex supports this:**  
  Yes. (Question does the normalization undo this or does this actually test the model?)

- **Input condition:**  

- **Description:**  
  This MR tests whether the model responds consistently to an additive offset in the input. If we increase every value in the input series by a constant $k$, the forecast should increase by approximately the same amount across all forecast steps.

- **Comment:**  
  Useful for detecting internal normalization or clipping issues that ignore absolute scale.


### MR-TS-02 — Scaling Equivariance

- **Input transformation:**  
  $x'(t) = a \cdot x(t), \quad a > 0$

- **Validation check:**  
  $f(x')(t) \approx a \cdot f(x)(t) \quad \text{for all } t \text{ in forecast horizon}$

- **Pass/Fail criteria:**  
  Pass if $|f(x')(t) - a \cdot f(x)(t)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model outputs forecasts in the same scale as the input and is not intentionally scale-invariant.

- **TiRex supports this:**  
  Yes. (Question does the normalization undo this or does this actually test the model?)

- **Input condition:**  
  Need to check if this is applicable for negative input values as well.

- **Description:**  
  This MR tests whether the model responds proportionally to a multiplicative scaling of the input. If all input values are multiplied by a constant factor $a$, the forecast should also scale by approximately the same factor across all forecast steps.

- **Comment:**  
  Useful for detecting internal normalization or clipping issues that ignore proportional scale.


### MR-TS-03 — Affine Equivariance (Scale + Shift)

- **Input transformation:**  
  $x'(t) = a \cdot x(t) + k$

- **Validation check:**  
  $f(x')(t) \approx a \cdot f(x)(t) + k \quad \text{for all } t \text{ in forecast horizon}$

- **Pass/Fail criteria:**  
  Pass if $|f(x')(t) - (a \cdot f(x)(t) + k)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model outputs forecasts in the same scale and units as the input, and is not intentionally scale-invariant or normalized in a way that cancels additive or multiplicative transformations.

- **TiRex supports this:**  
  Yes. (Question does the normalization undo this or does this actually test the model?)

- **Input condition:**  
  Applicable for series where both additive and multiplicative changes are meaningful and do not violate model assumptions (e.g., non-negative constraints or log-transforms).

- **Description:**  
  This MR tests whether the model responds consistently to a combination of multiplicative scaling and additive offset. If input values are scaled by $a$ and shifted by $k$, the forecast should change proportionally and additively across all forecast steps.

- **Comment:**  
  Useful for detecting issues where preprocessing (like z-score normalization) or de-normalization forgets the shift or scaling, leading to only partial affine response. Combines MR-TS-01 and MR-TS-02 in one test (Maybe this could be a motivation for combinatorial combinations of MRs).


### MR-TS-04 — Constant-series preservation
REMOVED

### MR-TS-05 — Duplicate-Last-Value Extension (Context Padding Stability)

- **Input transformation:**  
  $x' = [\, x, \underbrace{x(T), \dots, x(T)}_{m \text{ repeats}} \,]$

- **Validation check:**  
  $f(x') \approx f(x)$

- **Pass/Fail criteria:**  
  Pass if $|f(x')(t) - f(x)(t)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model uses the most recent context consistently and is robust to variable-length input sequences.

- **TiRex supports this:**  
  Yes

- **Input condition:**  
  Best applied when the last value is representative of the recent trend and when repeated values do not introduce unrealistic bias (i.e., avoid extreme peaks or anomalies that could dominate the forecast).

- **Description:**  
  This MR tests whether the forecast remains stable when the most recent observed value is repeated multiple times at the end of the input series. Ideally, adding duplicates of the last value should not significantly alter the forecast.

- **Comment:**  
  This MR can be **flaky** depending on the last value and the number of repeats, making it a candidate for additional input conditions or thresholds in the future. Useful for detecting issues with positional encodings, context window handling, or autoregressive sequence interpretation.


### MR-TS-06 — Missing-Value Imputation Invariance (Masked Inputs)

- **Input transformation:**  
  Replace missing entries with different fillers while keeping the same missing-mask:  
  $x_{\text{filled1}}$, $x_{\text{filled2}}$ with mask $m$.

- **Validation check:**  
  $f(x_{\text{filled1}}, m) \approx f(x_{\text{filled2}}, m)$

- **Pass/Fail criteria:**  
  Pass if $|f(x_{\text{filled1}}, m)(t) - f(x_{\text{filled2}}, m)(t)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model correctly uses the explicit missing-mask (or documented missing-value handling) so that filler values do not dominate the forecast.

- **TiRex supports this:**  
  Unclear. (Question does TiRex support input masking?) Yes with passing NAN values.

- **Input condition:**  

- **Description:**  
  This MR tests whether the forecast is invariant to the choice of filler values for missing entries, provided the model has access to a missing-mask. Changing fillers (e.g., 0 vs mean) should not significantly affect the output if the mask is correctly handled.

- **Comment:**  
  Important for models that handle missing data explicitly. If TiRex cannot use missing-masks, this MR may not be applicable and should be skipped or adapted. Could be tested indirectly by observing robustness to different fillers in practice.


### MR-TS-07 — Time-Shift Equivariance (Index-Origin Shift)

- **Input transformation:**  
  Shift timestamps by $\Delta$ while keeping the values the same:  
  $x'(t+\Delta) = x(t)$

- **Validation check:**  
  Forecast values should match for the same relative horizon:  
  $f(x')(t) \approx f(x)(t)$

- **Pass/Fail criteria:**  
  Pass if $|f(x')(t) - f(x)(t)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model should be time-origin invariant (no absolute-calendar features) or shift any calendar features consistently.

- **TiRex supports this:**  
  No — TiRex assumes sequences of numbers are uniformly spaced in time and does not accept explicit timestamps, so this MR cannot be applied directly.

- **Input condition:**  
  Applicable only for models that take absolute time or non-uniform timestamps as input.

- **Description:**  
  This MR tests whether the forecast is invariant to shifting the time index of the input series. The forecast should depend only on the relative temporal structure, not the absolute start time.

- **Comment:**  
  Not applicable for TiRex. Useful for models with calendar-aware features or non-uniform time indexing. 


### MR-TS-08 — Seasonal Phase-Shift Consistency

- **Input transformation:**  
  Circularly shift the input series by $p$ steps for a known period $P$:  
  $x'(t) = x((t+p) \bmod P)$

- **Validation check:**  
  Forecast should shift accordingly in phase:  
  $f(x')(t) \approx f(x)((t+p) \bmod P)$

- **Pass/Fail criteria:**  
  Pass if $|f(x')(t) - f(x)((t+p) \bmod P)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model leverages seasonal/periodic structure in the input and maintains phase relationships in its forecast.

- **TiRex supports this:**  
  Yes.

- **Input condition:**  
  Applicable only for inputs that exhibit strong periodicity with a known period $P$.

- **Description:**  
  This MR tests whether the forecast preserves the seasonal phase when the input series is circularly shifted by full periods. The predicted seasonality should align correctly after the shift.

- **Comment:**  
  Conditional MR — best applied when seasonality is evident and $p$ is well-defined.

### MR-TS-09 — Downsample Consistency

- **Input transformation:**  
  Downsample the input series by a factor $r$ and then interpolate back to the original length:  
  $x' = \text{interpolate}(\text{downsample}(x, r))$

- **Validation check:**  
  Forecast should remain approximately unchanged:  
  $f(x') \approx f(x)$

- **Pass/Fail criteria:**  
  Pass if $|f(x')(t) - f(x)(t)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  

- **TiRex supports this:**  
  Yes.

- **Input condition:**  
  Applicable only when the series is smooth enough and resampling does not introduce aliasing or artifacts.

- **Description:**  
  This MR tests whether the forecast is robust to mild resampling of the input. The forecast should not change significantly if the input is downsampled and interpolated back to the original length.

- **Comment:**  
  Helps detect models that are overly sensitive to small interpolation artifacts or discretization errors in the input series.


### MR-TS-09-01 — Upsample Consistency

- **Input transformation:**  
  Upsample the input series by a factor $r$ (insert interpolated points between original values) and then downsample back to the original length:  
  $x' = \text{downsample}(\text{upsample}(x, r))$

- **Validation check:**  
  Forecast should remain approximately unchanged:  
  $f(x') \approx f(x)$

- **Pass/Fail criteria:**  
  Pass if $|f(x')(t) - f(x)(t)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  

- **TiRex supports this:**  
  Yes. (Question: Do we also need to change the forecasting window to fit the higher sample rate reflected in the output?)

- **Input condition:**  
  Applicable only when the series is smooth enough and interpolation does not introduce artifacts or aliasing.

- **Description:**  
  This MR tests whether the forecast is robust to mild upsampling of the input. The forecast should not change significantly if the input is upsampled (interpolated) and then downsampled back to its original length.

- **Comment:**  
  Complements MR-TS-09 (downsample consistency) by testing the reverse transformation. Helps detect models overly sensitive to inserted/interpolated points.


### MR-TS-10 — Local Smoothing Robustness

- **Input transformation:**  
  Apply a moving average to smooth the input series over a window of size $w$:  
  $x' = \text{moving-average}(x, w)$

- **Validation check:**  
  Forecast should remain approximately consistent with the original trend:  
  $f(x') \approx f(x)$

- **Pass/Fail criteria:**  
  Pass if $|f(x')(t) - f(x)(t)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Forecasting task focuses on trend-level predictions rather than spike-level details.

- **TiRex supports this:**  
  Yes.

- **Input condition:**  
  Input series should contain noise or high-frequency fluctuations that can be smoothed without removing essential trend information.

- **Description:**  
  This MR tests whether the forecast is robust to small, local smoothing of the input. Smoothing should not significantly alter the predicted trend.

- **Comment:**  
  Useful for detecting models that overfit noise or spikes, resulting in unstable forecasts when minor smoothing is applied. We could derive similar MRs using other smoothing functions.


### MR-TS-11 — Noise Injection Robustness (Small Gaussian Noise)

- **Input transformation:**  
  Add small Gaussian noise to the input series:  
  $x'(t) = x(t) + \eta(t), \quad \eta(t) \sim \mathcal{N}(0, \sigma^2)$

- **Validation check:**  
  Forecast should remain approximately stable:  
  $f(x') \approx f(x)$

- **Pass/Fail criteria:**  
  Pass if $|f(x')(t) - f(x)(t)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  
- **TiRex supports this:**  
  Yes.

- **Input condition:**  
  Noise standard deviation $\sigma$ should be small relative to the signal scale so that injected noise does not dominate the forecast. Applicable to series where small additive perturbations are reasonable and do not push the input outside valid ranges.

- **Description:**  
  This MR tests whether the forecast is robust to small, random perturbations in the input. Adding minor Gaussian noise should not substantially alter the predicted values.

- **Comment:**  
  Essentially the inverse of MR-TS-10 (local smoothing). Useful for detecting models that are overly sensitive or numerically unstable. Can be generalized to other small perturbations besides Gaussian noise.


### MR-TS-12 — Outlier Clipping Robustness

- **Input transformation:**  
  Clip extreme points above a specified percentile (Winsorize) in the input series:  
  $x'(t) = \text{clip}(x(t), \text{lower}, \text{upper})$

- **Validation check:**  
  Forecast should remain approximately stable:  
  $f(x') \approx f(x)$

- **Pass/Fail criteria:**  
  Pass if $|f(x')(t) - f(x)(t)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model should not rely excessively on rare extreme values in the input.

- **TiRex supports this:**  
  Yes.

- **Input condition:**  
  Applicable when the input contains rare outliers that are reasonable to clip without affecting the main signal dynamics.

- **Description:**  
  This MR tests whether the forecast is robust to extreme input values. Clipping outliers should not drastically change the forecast if the model focuses on the main trend rather than being dominated by rare extremes.

- **Comment:**  
  Conceptually similar to MR-TS-10 (local smoothing), but instead of smoothing noise, this handles extreme values explicitly. Can reveal models overly sensitive to single extreme points.


### MR-TS-13 — Last-Step Perturbation Locality

- **Input transformation:**  
  Change only the last observed value by a small amount $\Delta$:  
  $x'(T) = x(T) + \Delta$, all other $x'(t) = x(t)$

- **Validation check:**  
  Early forecast steps may shift slightly, but the overall forecast should stay approximately stable:  
  $f(x') \approx f(x)$

- **Pass/Fail criteria:**  
  Pass if differences remain small and reasonable across the forecast horizon:  
  $|f(x')(t) - f(x)(t)| \le \epsilon$ for all $t$; fail otherwise.

- **Model requirements:**  
  Model should not treat the last input value as a global anchor that dominates all future steps.

- **TiRex supports this:**  
  Yes.

- **Input condition:**  
  Perturbation $\Delta$ should be small relative to the scale of the series.

- **Description:**  
  This MR tests whether a small change in the last input affects only nearby forecast steps, rather than causing large unrealistic changes in distant steps.

- **Comment:**  
  Useful for detecting models that overfit to the last input value and produce unstable long-horizon forecasts.


### MR-TS-14 — Normalization Round-Trip Consistency

- **Input transformation:**  
  Apply normalization (e.g., standardization) and then invert it using the same parameters:  
  $x' = \text{denormalize}(\text{normalize}(x))$

- **Validation check:**  
  Forecast should remain approximately unchanged:  
  $f(x') \approx f(x)$

- **Pass/Fail criteria:**  
  Pass if $|f(x')(t) - f(x)(t)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model preprocessing must be deterministic; otherwise, differences may appear due to stochastic transformations.

- **TiRex supports this:**  
  Yes, if deterministic preprocessing is applied. 

- **Input condition:**  
  Input can be any series compatible with the normalization procedure (e.g., avoids division by zero for zero-variance series).

- **Description:**  
  This MR tests whether a normalization followed by its inverse leaves the forecast unchanged. It primarily checks the correctness of preprocessing pipelines rather than the forecasting model itself.

- **Comment:**  
  Use this MR to detect inconsistencies or numerical errors in preprocessing and inverse transformations. This MR is more of a sanity check of the normalization and denormalization steps.


### MR-TS-15 — Permutation Invariance of Irrelevant Covariates

- **Input transformation:**  
  Permute covariate channels that are known to be unused or constant:  
  $x' = \text{permute}(x_\text{cov})$

- **Validation check:**  
  Forecast should remain approximately unchanged:  
  $f(x', x_\text{main}) \approx f(x, x_\text{main})$

- **Pass/Fail criteria:**  
  Pass if $|f(x', x_\text{main})(t) - f(x, x_\text{main})(t)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model must correctly ignore irrelevant covariates or handle masking appropriately.

- **TiRex supports this:**  
  No — TiRex only accepts a single input sequence of numbers and does not handle separate covariate channels.

- **Input condition:**  
  Applicable only for multivariate input series where some channels are known to be irrelevant or constant.

- **Description:**  
  This MR tests whether permuting irrelevant input channels affects the forecast. If covariates are truly irrelevant, the output should remain stable regardless of their ordering.

- **Comment:**  
  Not applicable for TiRex experiments. Useful for models that accept multivariate inputs and have separate covariate channels. Detects channel-indexing or masking errors.


### MR-TS-16 — Channel Permutation Equivariance (Multivariate Outputs)

- **Input transformation:**  
  Permute input channels according to a permutation $\pi$:  
  $x' = \pi(x)$

- **Validation check:**  
  Forecast output channels should permute accordingly:  
  $f(x') = \pi(f(x))$

- **Pass/Fail criteria:**  
  Pass if all output channels follow the permutation correctly; fail otherwise.

- **Model requirements:**  
  Model must be designed to treat channels symmetrically and produce outputs per channel.

- **TiRex supports this:**  
  No — TiRex only accepts a single input sequence of numbers and does not handle separate channels.

- **Input condition:**  
  Applicable only for multivariate inputs where the model produces separate per-channel outputs.

- **Description:**  
  This MR tests whether permuting input channels results in the same permutation applied to the corresponding output channels. It ensures that the model does not rely on fixed channel positions.

- **Comment:**  
  Not applicable for TiRex experiments. Useful for models that handle multiple input channels and output predictions per channel. Can detect hard-coded channel indexing errors.


### MR-TS-17 — Unit Conversion Consistency (Equivalent to Scaling)

- **Input transformation:**  
  Convert input series to different units (e.g., °C → °F) and optionally convert forecast back to original units:  
  $x' = \text{unit-convert}(x)$

- **Validation check:**  
  Forecast converted back should match the original forecast:  
  $f(\text{unit-convert}^{-1}(x')) \approx f(x)$

- **Pass/Fail criteria:**  
  Pass if $|f(\text{unit-convert}^{-1}(x'))(t) - f(x)(t)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model should operate on raw input units and not assume fixed-scale learned units.

- **TiRex supports this:**  
  No — TiRex only takes sequences of numeric values without explicit units. This MR is effectively equivalent to MR-TS-02 (scaling equivariance) in TiRex.

- **Input condition:**  
  Applicable only when input series units can be meaningfully converted and back without introducing distortions.

- **Description:**  
  This MR tests whether the forecast remains consistent under a unit conversion of the input. After converting the forecast back, it should match the original forecast if the model handles scaling correctly.

- **Comment:**  
  Not applicable for TiRex as it does not handle units. Equivalent to scaling MR (MR-TS-02). Can be useful for unit-aware models to detect incorrect assumptions about linearity or scaling.


### MR-TS-18 — Log Transform Equivariance (Positive Series)

- **Input transformation:**  
  Apply a logarithmic transform to the input series and map forecast back with exponential:  
  $x' = \log(x)$, then $f_\text{raw}(x) \approx \exp(f_\text{log}(x'))$

- **Validation check:**  
  Forecast in raw space should match original forecast:  
  $\exp(f(x')) \approx f(x)$

- **Pass/Fail criteria:**  
  Pass if $|\exp(f(x')(t)) - f(x)(t)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model and transformation pipeline must support log-space operations correctly (e.g., bias correction if needed).

- **TiRex supports this:**  
  Unsure. (Question: Does TiRex include explicit log-transform pipelines?)

- **Input condition:**  
  Input series must be strictly positive ($x(t) > 0$) for all $t$; otherwise the log-transform is undefined and the MR cannot be applied.

- **Description:**  
  This MR tests whether the forecast is consistent under a log transformation of positive input values. After applying $\log$ to the input and predicting in log-space, mapping the forecast back with $\exp$ should reproduce the original forecast.

- **Comment:**  
  Conditional MR — cannot be used for series with non-positive values. Not suitable for automatic refinement with TiRex unless preprocessing guarantees positivity.


### MR-TS-19 — Differencing + Integration Consistency (Random-Walk-Like)

- **Input transformation:**  
  Apply first-order differencing to the series, forecast on differences, then integrate back to original scale:  
  $d(t) = x(t) - x(t-1)$, forecast $f(d)$

- **Validation check:**  
  Integrated forecast should match the original forecast:  
  $f(x) = \text{integrate}(f(d))$

- **Pass/Fail criteria:**  
  Pass if $|f_(x)(t) - \text{integrate}(f(d)(t))| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model must be able to handle differenced series correctly and produce forecasts that can be meaningfully integrated back.

- **TiRex supports this:**  
  Yes. 

- **Input condition:**  
  Input series should be reasonably smooth and not too noisy, so that differencing produces meaningful changes without overwhelming noise.

- **Description:**  
  This MR tests whether forecasting on first-order differences and integrating the forecast back reproduces the original forecast. It checks consistency of level vs. change handling in the model and ensures that differencing pipelines do not break long-term forecast paths.

- **Comment:**  
  Useful for models applied to random-walk-like or trend-dominated series. Can reveal issues where the model incorrectly interprets levels vs. changes.



### MR-TS-20 — Context Window Monotonicity (Longer History Stability)

- **Input transformation:**  
  Extend the input series by adding earlier observations as a prefix:  
  $x_\text{long} = [x_\text{past}, x]$

- **Validation check:**  
  Forecast on extended history should remain close to forecast on original history:  
  $f(x_\text{long}) \approx f(x)$

- **Pass/Fail criteria:**  
  Pass if $|f(x_\text{long})(t) - f(x)(t)| \le \epsilon$ for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model should leverage additional context without being destabilized by longer input sequences and handle variable-length inputs consistently.

- **TiRex supports this:**  
  Yes.

- **Input condition:**  
  Added historical context should be consistent and not contradictory to the existing input series. Avoid using this MR if prefix contains anomalies or conflicting trends.

- **Description:**  
  This MR tests whether adding more historical context improves or at least does not drastically worsen forecasts. The forecast should remain stable or improve slightly when more past data is provided.

- **Comment:**  
  Detects issues with attention mechanisms or context window handling in sequence models. Could be extended to test different prefix lengths to assess sensitivity to input history. Inverse MR could be removing earliest values from input.


### MR-TS-21 — Backcasting Symmetry Check (Time-Reversal)

- **Input transformation:**  
  Reverse the input series in time, forecast the “past,” then reverse the forecast back to original order:  
  $x_\text{rev} = \text{reverse}(x)$, forecast $f(x_\text{rev})$, then $f_\text{rev-back} = \text{reverse}(f(x_\text{rev}))$

- **Validation check:**  
  Reconstructed past values should match the tail of the original series:  
  $f_\text{rev-back} \approx x_\text{tail}$

- **Pass/Fail criteria:**  
  Pass if $|f_\text{rev-back}(t) - x_\text{tail}(t)| \le \epsilon$ for all steps; fail otherwise.

- **Model requirements:**  
  Model must claim time-reversal symmetry or the task must be inherently symmetric.

- **TiRex supports this:**  
  No (Question: Does TiRex support explicit backcasting.) Is used in training to improve performance so it should work.

- **Input condition:**  

- **Description:**  
  This MR tests whether a model can reconstruct recent past values by forecasting backward from a reversed series. It is intended for models that claim time-reversal invariance or for tasks that are symmetric in time.

- **Comment:**  
  Not a strict metamorphic test in the classic sense, since it does not compare outputs from two executions under a standard input transformation.


### MR-TS-22 — Quantile Monotonicity (Probabilistic Forecasts)

- **Input transformation:**  
  Identity (no change):  
  $x' = x$

- **Validation check:**  
  For each forecast horizon step, predicted quantiles should be ordered:  
  $Q_{0.1} \le Q_{0.5} \le Q_{0.9}$

- **Pass/Fail criteria:**  
  Pass if all quantiles maintain the correct order for every forecast step; fail if any crossing occurs.

- **Model requirements:**  
  Model must output multiple quantiles per horizon step (probabilistic forecast).

- **TiRex supports this:**  
  Yes. This should always apply, because TiRex does an internal quantile reordering.

- **Input condition:**  
  Applicable to series where probabilistic forecasts with multiple quantiles are meaningful.

- **Description:**  
  This MR checks that predicted quantiles do not cross, ensuring internal consistency of probabilistic forecasts. It serves as a sanity check rather than a classical metamorphic relation since there is no input transformation.

- **Comment:**  
  Useful for models with separate quantile heads or probabilistic outputs. NOT REALLY AN MR.


### MR-TS-23 — Prediction Interval Widening with Increased Noise

- **Input transformation:**  
  Add Gaussian noise with larger variance to the input series:  
  $x' = x + \eta_2, \quad \eta_2 \sim \mathcal{N}(0, \sigma_2^2), \quad \sigma_2 > \sigma_1$

- **Validation check:**  
  Prediction interval (PI) width should increase with noisier input:  
  $\text{PI-width}(x') \ge \text{PI-width}(x)$  
  where $\text{PI-width} = Q_{0.9} - Q_{0.1}$

- **Pass/Fail criteria:**  
  Pass if the PI width increases on average across horizon steps; fail if intervals shrink despite higher input noise.

- **Model requirements:**  
  Model must output probabilistic forecasts and respond to input uncertainty in a meaningful way (uncertainty head connected to input).

- **TiRex supports this:**  
  Yes.

- **Input condition:**  
  Applicable to input series where Gaussian noise can be added without producing invalid values (e.g., negative values if model assumes positivity).

- **Description:**  
  This MR tests whether a probabilistic forecasting model increases its prediction uncertainty when the input becomes noisier. Adding larger noise should result in wider prediction intervals, reflecting higher uncertainty.

- **Comment:**  
  Useful for models with explicit uncertainty modeling. Essentially checks the sensitivity of the uncertainty estimate to input perturbations.


### MR-TS-24 — Horizon Consistency (Prefix Agreement)

- **Input transformation:**  
  Request two forecasts with different horizons from the same input series:  
  $f_H(x)$ and $f_{H+K}(x)$ with $K > 0$

- **Validation check:**  
  The first $H$ steps of the longer forecast should match the shorter forecast:  
  $f_{H+K}(x)[1:H] \approx f_H(x)$

- **Pass/Fail criteria:**  
  Pass if $|f_{H+K}(x)(t) - f_H(x)(t)| \le \epsilon$ for all $t = 1, \dots, H$; fail otherwise.

- **Model requirements:**  
  Inference must be deterministic (or random seed controlled). The model should not let the requested horizon length influence earlier forecast steps.

- **TiRex supports this:**  
  Yes.

- **Input condition:**  

- **Description:**  
  This MR tests whether early forecast steps remain consistent when requesting a longer forecast horizon. Extending the horizon should not retroactively alter the initial predicted values.

- **Comment:**  
  Detects issues in autoregressive decoding loops or horizon-dependent conditioning logic. Comparing the prefix explicitly (i.e., `f_{H+K}(x)[:H]`) is the correct formulation.


### MR-TS-25 — Ensemble Averaging Sanity (Mixture Inputs)

- **Input transformation:**  
  Construct a new input series as the pointwise average of two similar series:  
  $x' = \frac{x_1 + x_2}{2}$

- **Validation check:**  
  Forecast of the averaged input should be close to the average of the individual forecasts:  
  $f(x') \approx \frac{f(x_1) + f(x_2)}{2}$

- **Pass/Fail criteria:**  
  Pass if  
  $|f(x')(t) - \frac{f(x_1)(t) + f(x_2)(t)}{2}| \le \epsilon$  
  for all forecast steps; fail otherwise.

- **Model requirements:**  
  Model should behave approximately linearly in the local region between $x_1$ and $x_2$. This is a soft MR and does not require strict linearity.

- **TiRex supports this:**  
  Yes.

- **Input condition:**  
  $x_1$ and $x_2$ should be similar in scale and structure. Extreme differences may invalidate the linearity assumption.

- **Description:**  
  This MR tests whether the model behaves consistently under convex combinations of similar inputs. If the model is locally smooth and approximately linear, forecasting the averaged input should yield an averaged forecast.

- **Comment:**  
  Soft metamorphic relation. Violations may indicate hidden normalization effects, non-linear saturation, or scale-dependent preprocessing.

### MR-TS-26 — Additive Offset Accuracy Preservation

- **Input transformation:**  
  $x'(t) = x(t) + c$  

- **Validation check:**  
  The forecast error against ground truth should not increase after the offset transformation:  
  $\text{MAE}(f(x'_\text{in}),\, x'_\text{gt}) - \text{MAE}(f(x_\text{in}),\, x_\text{gt}) \le \epsilon$

- **Pass/Fail criteria:**  
  Pass if the MAE increase does not exceed $\epsilon = 10^{-3}$; fail otherwise.  
  Median quantile is used as the point forecast for MAE computation.

- **Model requirements:**  
  Model must produce quantile forecasts from which a median can be derived. Inference must
  be deterministic. Series must be long enough to hold back a meaningful ground-truth window
  (at least $2 \times$ `prediction_length`).

- **TiRex supports this:**  
  Yes

- **Input condition:**  
  The offset $c$ should be chosen so the shifted series remains in a plausible value range
  for the model. Avoid offsets that push values outside the model's training distribution.

- **Description:**  
  This MR tests that adding a constant offset to the input does not degrade forecast
  accuracy. Since the ground truth is also shifted by $c$, a correct model should achieve
  the same MAE on both the original and the shifted series. An error increase indicates
  the model handles absolute input level inconsistently — for example, a de-normalization
  bug that produces a systematic bias only at certain value ranges.

- **Comment:**  
  Distinct from MR-TS-01 (Additive Offset Equivariance): MR-TS-01 checks that the two
  forecasts relate correctly to each other (no ground truth needed), while this MR checks
  that each forecast is accurate against its own ground truth. MR-TS-01 can pass even when
  both forecasts are equally wrong; this MR catches that case. The check is one-sided
  (error must not increase) so it will not flag cases where the transformation accidentally
  improves accuracy — a symmetric MAE-equality check would be stricter.

### MR-TS-27 — Scaling Accuracy Preservation

- **Input transformation:**  
  $x'(t) = a \cdot x(t), \quad a > 0$  

- **Validation check:**  
  The forecast error against ground truth should not increase after the scaling transformation:  
  $\text{MAE}(f(x'_\text{in}),\, x'_\text{gt}) - \text{MAE}(f(x_\text{in}),\, x_\text{gt}) \le \epsilon$

- **Pass/Fail criteria:**  
  Pass if the MAE increase does not exceed $\epsilon = 10^{-3}$; fail otherwise.  
  Median quantile used as the point forecast.

- **Model requirements:**  
  Same as MR-TS-26. Requires deterministic inference and a series long enough to provide
  a held-out ground-truth window.

- **TiRex supports this:**  
  Yes

- **Input condition:**  
  Scale factor $a > 0$. Avoid extremely large or small values of $a$ that push the series
  outside the model's training distribution, as these may cause failures unrelated to the
  MR's intent.

- **Description:**  
  This MR tests that multiplying the input by a positive factor does not degrade forecast
  accuracy relative to the correspondingly scaled ground truth. A correct model's MAE
  should be scale-proportional: the same relative forecasting skill should be retained
  regardless of the absolute magnitude of the series. A failure indicates that the model's
  preprocessing or de-normalization breaks down at certain scales.

- **Comment:**  
  Distinct from MR-TS-02 (Scaling Equivariance): MR-TS-02 checks the relationship between
  the two forecasts directly, while this MR checks each forecast independently against its
  own ground truth. The same one-sided caveat from MR-TS-26 applies: a symmetric
  MAE-equality check would be stricter and catch cases where scaling accidentally improves
  accuracy. Together, MR-TS-26 and MR-TS-27 form the ground-truth-backed counterparts to
  MR-TS-01 and MR-TS-02 respectively.

### MR-TS-28 — Recent Context Truncation Stability

- **Input transformation:**  
  Remove the last `steps` observations from the input series:  
  $x' = x[1 : T - \text{steps}]$

- **Validation check:**  
  The forecast from the truncated series should remain approximately equal to the forecast
  from the full series:  
  $f(x')(t, q) \approx f(x)(t, q) \quad \text{for all } t, q$

- **Pass/Fail criteria:**  
  Pass if $|f(x')(t, q) - f(x)(t, q)| \le \epsilon$ for all forecast steps $t$ and
  quantile indices $q$; fail at the first violation.  
  Default tolerance: $\epsilon = 10^{-3}$.

- **Model requirements:**  
  Model must accept variable-length input sequences. Inference must be deterministic.

- **TiRex supports this:**  
  Conditional — TiRex accepts variable-length sequences, but removing the most recent
  steps changes the effective forecast origin. Best applied when the removed tail points
  carry little additional signal (e.g., the series is locally stable near the end).

- **Input condition:**  
  `steps` should be small relative to the total series length. The truncated series must
  remain long enough to produce a meaningful forecast. Avoid applying this MR when the
  last `steps` values are highly informative (e.g., end of a sharp spike or trend change),
  as a forecast difference in that case is expected and correct.

- **Description:**  
  This MR tests whether removing a small number of the most recent observations destabilizes
  the forecast. A robust model should not change its forecast drastically when only a few
  recent, low-information tail points are dropped. It probes whether the model over-anchors
  on the very last observed values or whether its forecast is stable with respect to mild
  reductions in recent context.

- **Comment:**  
  This MR is the recent-end counterpart to MR-TS-20 (Context Window Monotonicity), which
  adds older history at the front. MR-TS-20's own comment mentions "removing earliest
  values" as an inverse — but this MR removes from the *recent* end, which is more
  disruptive and tests a different failure mode (over-reliance on the most recent point
  vs. over-reliance on distant history). The `ForecastInvariantCheck` (strict equality up
  to $\epsilon$) may be too tight in practice; a relaxed tolerance or a monotonicity-based
  check (forecast should not change *more* than a proportional bound) could reduce
  flakiness. The class name `TimeShift` in the source code is misleading — this is a tail
  truncation, not a calendar/timestamp shift (cf. MR-TS-07).


## Template (Baseline description)

### MR-TS-XX — Short Descriptive Title

- **Input transformation:**  
  Mathematical definition of how the input is transformed:  
  $x' = T(x)$  
  (Clearly specify the transformation operator and any parameters.)

- **Validation check:**  
  Mathematical condition that must hold between original and transformed outputs:  
  $R\big(f(x'), f(x)\big)$  
  (State the expected relationship precisely, e.g., equality, approximation, monotonicity, permutation, etc.)

- **Pass/Fail criteria:**  
  Define the concrete numerical or logical condition used for evaluation. Example:  
  Pass if $|f(x')(t) - f(x)(t)| \le \epsilon$ for all forecast steps; fail otherwise.  
  (Specify tolerance $\epsilon$, aggregation method, horizon scope, or statistical test if applicable.)

- **Model requirements:**  
  Specify structural or functional properties the model must satisfy for the MR to be meaningful  
  (e.g., probabilistic outputs, multivariate inputs, deterministic inference, symmetry assumptions).

- **TiRex supports this:**  
  Yes / No / Conditional (brief justification if conditional).

- **Input condition:**  
  Constraints on the input under which the MR is valid  
  (e.g., strictly positive values, smoothness assumptions, absence of anomalies, compatible scale).

- **Description:**  
  Plain-language explanation of what the MR tests and why the expected relation should hold.

- **Comment:**  
  Additional notes, limitations, related MRs, implementation guidance, or recommendation to include/exclude from an automated MR library.

---
