# Early Loading Screen

A Fabric mod that shows an early loading screen and display information while the game is loading. 

## Compatibility
This mod has been tested with AOF6 with Prism Launcher and default settings without any issues.

## Configuration

All configuration is done in the `early-loading-screen.properties` file in the config folder.

### `window_creation_point`
Available options: `mixinEarly` `mixinLoad` `preLaunch` `mcEarly` `off`  
Default: `mixinEarly` (or `mixinLoad` if ImmediatelyFast is installed)

This controls the point when the window is created. 
The available options above are sorted in order of earliest to latest.
The option `mixinEarly` does some classloading hacks, use `mixinLoad` if you have issues with it.

Use `off` to turn off early screen entirely.

### `enable_entrypoint_information`
Available options: `true` `false`
Default: `true`

This controls whether the entrypoint information is shown on the early screen.
When this is enabled, it will apply patches to the fabric-loader to show entrypoint invocation process.
Disable if you have weird issues with the game launch.

### `reuse_early_window`
Available options: `true` `false`
Default: `true`
Not available on Windows.

This controls whether the early screen is reused as the game window.

### `enable_mixin_pretransform`
Available options: `true` `false`
Default: `false`

This controls whether mixins apply runs early. 
May cause issues with mods that doesn't handle classloading properly.

### `allow_early_window_close`
Available options: `true` `false`
Default: `true`

This controls whether the early screen can be closed.
Closing the early screen will cause the game to exit immediately.
