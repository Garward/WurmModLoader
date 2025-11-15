package com.garward.wurmmodloader.modloader.interfaces;

/**
 * Legacy compatibility interface for Wurm Unlimited mods.
 * 
 * <p>This interface serves as a backward compatibility layer for mods that were
 * originally written for older versions of the Wurm modding API. It extends
 * the newer WurmServerMod interface to provide a migration path for existing
 * mod code.</p>
 * 
 * <p>Usage example:
 * <pre>@code
 * public class MyLegacyMod implements WurmMod {
 *     // Implementation here
 * }
 * </pre></p>
 * 
 * <p><strong>Thread Safety:</strong> Implementations of this interface should
 * follow the same thread safety guidelines as WurmServerMod.</p>
 * 
 * <p><strong>Lifecycle:</strong> This interface is deprecated and will be
 * removed in future versions. Mods should migrate to implementing
 * WurmServerMod directly.</p>
 * 
 * @since 1.0.0
 * @deprecated Use WurmServerMod directly instead
 * @see WurmServerMod
 */
/**
 * Compatibility interface
 */
@Deprecated
public interface WurmMod extends WurmServerMod {

}
