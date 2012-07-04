package ca.uwinnipeg.proximitydroid.services;

/**
 * An object with an epsilon.
 * @author Garrett Smith
 *
 */
public interface EpsilonProperty {

  /**
   * Returns the epsilon used to calculate the property.
   * @return
   */
  public abstract float getEpsilon();

  /**
   * Set the epsilon to calculate the property with.
   * @param epsilon
   */
  public abstract void setEpsilon(float epsilon);

}