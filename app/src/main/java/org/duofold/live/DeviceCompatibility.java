package org.duofold.live;

/** Eligibility is not proof of a firmware's Samsung wallpaper/API compatibility. */
public final class DeviceCompatibility {
 private DeviceCompatibility() {}
 public static boolean isFold8(String model) {
  return model != null && model.matches("SM-F971[A-Z0-9]+(?:/DS)?");
 }
 public static boolean isRecognized(String model) {
  return model != null && model.matches("SM-F(?:971|976|966|946)[A-Z0-9]+(?:/DS)?");
 }
 public static String modelWarning(String model) {
  return isRecognized(model) ? "" : "Unrecognized device: " + String.valueOf(model)
   + ". You can continue, but compatibility is unverified and features may not work. Samsung wallpaper and API checks still apply.";
 }
 public static boolean isEligible(String model, int sdk) {
  return sdk >= 36;
 }
 public static void requireEligible(String model, int sdk) {
  if (!isEligible(model, sdk)) throw new IllegalStateException(
   "This Samsung profile requires Android 16 or newer; received " + model + " / SDK " + sdk);
 }
}
