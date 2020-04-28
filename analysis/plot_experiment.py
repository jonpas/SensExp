#!/usr/bin/env python3

import argparse
import csv

import numpy as np
import matplotlib.pyplot as plt
from pydub import AudioSegment


# Parse aguments
parser = argparse.ArgumentParser()
parser.add_argument("audio", help="audio recording (3GP)")
parser.add_argument("accel", help="accelerometer recording (CSV)")
args = parser.parse_args()

experiment_name = args.audio.split("_")[1]

# Prepare plot
fig, (ax1, ax2) = plt.subplots(nrows=2, ncols=1)
fig.suptitle(f"SensExp Analysis ({experiment_name})")
plt.tight_layout(rect=[0, 0.03, 1, 0.95])


# Plot audio
audio = AudioSegment.from_file(args.audio, format="3gp")
audio_samples = audio.get_array_of_samples()

time = np.linspace(0, len(audio_samples) / audio.frame_rate, num=len(audio_samples))
ax1.plot(time, audio_samples)

ax1.axhline(linewidth=1, color="grey")
ax1.set_ylabel("Amplitude")

# Plot accelerometer
timestamps, x, y, z, prompts = [], [], [], [], []
with open(args.accel, "r", encoding="utf-8") as f:
    rows = csv.reader(f, delimiter=" ")

    for row in rows:
        timestamps.append(int(row[0]) / 1000)
        x.append(float(row[1]))
        y.append(float(row[2]))
        z.append(float(row[3]))
        prompts.append(row[4] == "true")

max_accel = max(max(x), max(y), max(z))
prompts = [max_accel if x else 0 for x in prompts]

ax2.plot(timestamps, x)
ax2.plot(timestamps, y)
ax2.plot(timestamps, z)
ax2.plot(timestamps, prompts)

ax2.legend(["X", "Y", "Z", "Fire Prompt"])
ax2.axhline(linewidth=1, color="grey")
ax2.set_ylabel("Linear Acceleration")
ax2.set_xlabel("Time (s)")

plt.subplots_adjust(hspace=0, wspace=0)
plt.show()
