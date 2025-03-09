#!/bin/bash

# Sizes for different densities
declare -A sizes=(
  ["mdpi"]=48
  ["hdpi"]=72
  ["xhdpi"]=96
  ["xxhdpi"]=144
  ["xxxhdpi"]=192
)

# Create icons for each density
for density in "${!sizes[@]}"; do
  size=${sizes[$density]}
  
  # Create background
  convert -size ${size}x${size} xc:#6200EE \
    \( -size ${size}x${size} xc:none -fill white -draw "path 'M ${size}/4,${size}/8 v ${size}/2.5 c -0.59*${size}/24,-0.34*${size}/24 -1.27*${size}/24,-0.55*${size}/24 -2*${size}/24,-0.55*${size}/24 -2.21*${size}/24,0 -4*${size}/24,1.79*${size}/24 -4*${size}/24,4*${size}/24 s 1.79*${size}/24,4*${size}/24 4*${size}/24,4*${size}/24 4*${size}/24,-1.79*${size}/24 4*${size}/24,-4*${size}/24 V ${size}/3 h 4*${size}/24 V ${size}/8 h -6*${size}/24 z'" \) \
    -composite \
    "app/src/main/res/mipmap-${density}/ic_launcher.png"

  # Create round version (same icon but with circular mask)
  convert -size ${size}x${size} xc:#6200EE \
    \( -size ${size}x${size} xc:none -fill white -draw "path 'M ${size}/4,${size}/8 v ${size}/2.5 c -0.59*${size}/24,-0.34*${size}/24 -1.27*${size}/24,-0.55*${size}/24 -2*${size}/24,-0.55*${size}/24 -2.21*${size}/24,0 -4*${size}/24,1.79*${size}/24 -4*${size}/24,4*${size}/24 s 1.79*${size}/24,4*${size}/24 4*${size}/24,4*${size}/24 4*${size}/24,-1.79*${size}/24 4*${size}/24,-4*${size}/24 V ${size}/3 h 4*${size}/24 V ${size}/8 h -6*${size}/24 z'" \) \
    -composite \
    \( -size ${size}x${size} xc:black -draw "circle ${size}/2,${size}/2 ${size}/2,0" -alpha copy \) \
    -compose CopyOpacity -composite \
    "app/src/main/res/mipmap-${density}/ic_launcher_round.png"
done 