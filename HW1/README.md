# HW 1

## Part 1: Theory (nosubmit)

TODO

## Part 2: Programming Questions

Compile & run

```
javac ImageDisplay.java && java ImageDisplay [FILE_PATH] [S] [A] [w]
```

No errors (compile oe runtime) are expected 👍

## Part 3: Analysis Question

Each image was subject to missing sample x = [10, 20, 30, 40, 50] percent. All images were plotted into 1 file, `graphs.png` for aggregate analysis.

From the graph, `worldmap` has the highest error, mostly because there is an amalgamation of colors all over the map which makes nearest-neighbors a poor indicator of the original color present before the pixel was extracted.

On the other hand, `rubixcube` has the lowest error, most likely due to its predominantly black background, which makes nearest-neighbors a solid indicator of the original color present.

From these observations, it is a relatively safe speculation to say that images with not many colors would not suffer too much from interpolation and filtering, whereas those with a variety of colors would suffer more.
