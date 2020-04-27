# FERI-SensExp

Simple experimental application for gathering data for later analysis in a sensor experiment on Android at Ubiquitous Computing (slo. Vseprisotno Računalništvo).

Goal of the experiment is to prove or disprove the ability to accurately sense a shot has been taken with a gas airsoft pistol using a regular Android phone and its on-board sensors (microphone and linear accelerometer).

### Usage

- Run application
- Set experiment name _(optional)_
- Toggle `Capture` switch and take a shot when it displays "Fire!"
- Toggle `Capture` switch when enough data was captured
- Experiment data saves to `/storage/emulated/0/Android/data/com.jonpas.sensexp/files/`
- Copy captured data from `/storage/emulated/0/Android/data/com.jonpas.sensexp/files` to `analysis` folder
- Analyse by following the analysis instructions below

### Setup

**Requirements:**
- [Android Studio](https://developer.android.com/studio)


## Analysis

Simple Python script to plot audio and linear accelerometer data for further analysis (`analysis` folder).

### Usage

**Setup:**

- `$ python -m venv venv` (virtual environment)
- `$ source venv/bin/activate`
- `$ pip install -r requirements.txt` (`$ pip freeze > requirements.txt` to update dependencies)

**Run:**
- `$ python plot_experiment.py <name>.3gp <name>.csv`
