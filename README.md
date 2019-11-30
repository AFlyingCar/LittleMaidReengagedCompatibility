# LittleMaidReengaged Compatibility

## What's this?
A compatibility mod between [my fork of LittleMaidReengaged](https://github.com/AFlyingCar/LittleMaidReengaged) and various other mods.

Note that this mod is using features from the `compatibility_features` branch of the fork.

## LICENSE
Read LICENSE.md

## Making developing environment
```shell_script
./gradlew setupDecompWorkspace
```

Make sure there is a `libs/` folder containing the mod files for `chickens-6.1.0`, `roost-1.12.2-2.0.10`, `techguns-1.12.2-2.0.2.0`, `EBLib`, and the forked version of LittleMaidReengaged.

If you would like to use an IDE, run _one_ of the following two commands:
```shell script
./gradlew idea
./gradlew eclipse
```
