package karn.minecraftsnap.config;

public class UnitExtraAttributes {
	public Double jumpStrength;
	public Double stepHeight;
	public Double scale;
	public Double attackRange;
	public Double knockbackResistance;
	public Double safeFallDistance;

	public void normalize() {
		if (jumpStrength != null && jumpStrength.isNaN()) {
			jumpStrength = null;
		}
		if (stepHeight != null && stepHeight.isNaN()) {
			stepHeight = null;
		}
		if (scale != null && scale.isNaN()) {
			scale = null;
		}
		if (attackRange != null && attackRange.isNaN()) {
			attackRange = null;
		}
		if (knockbackResistance != null && knockbackResistance.isNaN()) {
			knockbackResistance = null;
		}
		if (safeFallDistance != null && safeFallDistance.isNaN()) {
			safeFallDistance = null;
		}
	}

	public double jumpStrengthOrDefault(double fallback) {
		return jumpStrength == null ? fallback : jumpStrength;
	}

	public double stepHeightOrDefault(double fallback) {
		return stepHeight == null ? fallback : stepHeight;
	}

	public double scaleOrDefault(double fallback) {
		return scale == null ? fallback : scale;
	}

	public double attackRangeOrDefault(double fallback) {
		return attackRange == null ? fallback : attackRange;
	}

	public double knockbackResistanceOrDefault(double fallback) {
		return knockbackResistance == null ? fallback : knockbackResistance;
	}

	public double safeFallDistanceOrDefault(double fallback) {
		return safeFallDistance == null ? fallback : safeFallDistance;
	}
}
