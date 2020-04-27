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


# Plot accelerometer
with open(args.accel, "r", encoding="utf-8") as f:
    rows = csv.reader(f, delimiter=" ")

    timestamps, x, y, z = [], [], [], []
    for row in rows:
        timestamps.append(int(row[0]) / 1000)
        x.append(float(row[1]))
        y.append(float(row[2]))
        z.append(float(row[3]))

    ax2.plot(timestamps, x)
    ax2.plot(timestamps, y)
    ax2.plot(timestamps, z)


ax1.axhline(linewidth=1, color="grey")
ax1.set_ylabel("Amplitude")

ax2.axhline(linewidth=1, color="grey")
ax2.set_ylabel("Change")
ax2.set_xlabel("Time (ms)")

plt.subplots_adjust(hspace=0, wspace=0)
plt.show()
