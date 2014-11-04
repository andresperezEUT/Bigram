////////////////////////////////////////////////////////////////////////////
//
// Copyright (C) Andrés Pérez López, November 2014 [contact@andresperezlopez.com]
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; withot even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <<http://www.gnu.org/licenses/>
//
////////////////////////////////////////////////////////////////////////////

BigramEditor {

	var <bigramTracks; // references to all tracks:
	var <bigramTrackNames; // names of all tracks, in the same position as its correspondent bigramTrack
	var <numTracks = 0;
	var numTotalTracks = 0; // only for provisional names


	// time-related
	var <tempos; // a different Integer for each pulse
	var <>defaultTempo = 120; //in bpm

	var <measures; // a different PD value for each bar
	var <>defaultMeasure; // PD(4,2), initialize in init

	var <measuresFromBars; // bars:[PD,PD...]
	var <numBars;

	var <defaultNumPulses = 32; // total number of pulses

	var <barsFromPulses; // list with the bar number for each pulse

	var <numOctaves = 8;

	var <pattern;
	var <eventStreamPlayer;
	var <eventStreamPlayerController;
	var <bigramEditorWindow;


	*new { |editorWindow|
		^super.new.init(editorWindow);
	}

	init { |editorWindow|

		bigramEditorWindow = editorWindow;

		bigramTracks = List.new;
		bigramTrackNames = List.new;

		tempos = List.fill(defaultNumPulses,{defaultTempo});

		defaultMeasure = PD(4,2);
		measures = List.fill(defaultNumPulses,{defaultMeasure});

		barsFromPulses = List.new;
		defaultNumPulses.do { |i|
			barsFromPulses.add((i/defaultMeasure.pulse).floor);
		};

		numBars = (defaultNumPulses/defaultMeasure.pulse).ceil;
		measuresFromBars = List.newUsing(PD()!numBars);

	}





	///////////////////////// TIME MANAGING /////////////////////////

	//setTempo: set new tempo for all divisions starting on arg
	setTempo { |newTempo, pulse|
		var newArray = tempos[..pulse-1] ++ (tempos[pulse..].collect(newTempo));
		tempos = List.newUsing(newArray);
	}

	//setTempoInRange: set new tempo for divisons in the range [,) (first included)
	setTempoInRange { |newTempo, pulse1, pulse2|
		var newArray = tempos[..pulse1-1] ++ (tempos[pulse1..pulse2-1].collect(newTempo)) ++ tempos[pulse2..];
		tempos = List.newUsing(newArray);
	}

	//setBar: changes a bar
	setBarPD { |pulseIndex,newPD|
		// things to change: barsFromPulses,measuresFromBars

		var pulses = newPD.pulse;
		var pulseDiff;
		var lastPulseInBar;

		// 1: locate in which bar is that pulse
		var barNumber = this.getBarNumber(pulseIndex);

		// 2: store how many pulses were before
		var oldPulses = measuresFromBars.at(barNumber).pulse;

		// 3 change PD in measuresFromBars
		measuresFromBars.put(barNumber,newPD);

		// 4: change barsFromPulses values
		pulseDiff = pulses - oldPulses;

		// get last pulse of the bar
		lastPulseInBar = this.getLastPulseIndexFromBar(barNumber);

		switch(pulseDiff)
		{-1} { //remove numbers from bar end
			pulseDiff.abs.do{ |i| barsFromPulses.removeAt(lastPulseInBar-i) };
		}
		{0} { //do nothing

		}
		{1} { //add number at bar end
			pulseDiff.do{ barsFromPulses.insert(lastPulseInBar+1,barNumber) };
		};


	}

	// in which bar is that pulse?
	getBarNumber { |pulseIndex|
		^barsFromPulses[pulseIndex];
	}

	// given a bar number, get the pulseIndex of the last pulse in the bar
	getLastPulseIndexFromBar { |barNumber|
		^barsFromPulses.indicesOfEqual(barNumber).last;
	}

	numPulses {
		^barsFromPulses.size;
	}

	// following methods give a division index of 0
	getBPDfromPulseIndex { |pulseIndex|

		var bar = this.getBarNumber(pulseIndex);
		var pulse = barsFromPulses.indicesOfEqual(bar).indexOf(pulseIndex);
		var division = 0;

		^BPD.new(bar,pulse,division);
	}

	getPulseIndexFromBPD { |bpd|
		^barsFromPulses.indicesOfEqual(bpd.bar).at(bpd.pulse);
	}

	getSubdivisionIndexFromBPD { |bpd|
		var pulseIndex = this.getPulseIndexFromBPD(bpd);
		var subdivision = 0;
		pulseIndex.do { |index|
			var bar = barsFromPulses.at(index);
			subdivision = subdivision + measuresFromBars.at(bar).division;
			/*			var pd = measuresFromBars.at(index).postln;
			subdivision = subdivision + pd.division;*/
		};
		^subdivision;
	}





	///////////////////////// TRACK MANAGING /////////////////////////


	addTrack {
		var name = numTotalTracks.asSymbol;
		bigramTracks.add(BigramTrack.new(this,name));
		bigramTrackNames.add(numTotalTracks.asSymbol);

		numTracks = numTracks + 1;
		numTotalTracks = numTotalTracks + 1;
		^name;
	}

	duplicateTrack { |trackName|
		var originalIndex = bigramTrackNames.indexOf(trackName);
		var originalTrack = bigramTracks.at(originalIndex);

		var name = numTotalTracks.asSymbol;
		var newTrackIndex = numTracks;
		bigramTracks.add(BigramTrack.new(this,name));
		bigramTrackNames.add(numTotalTracks.asSymbol);


		numTracks = numTracks + 1;
		numTotalTracks = numTotalTracks + 1;

		//copy contents

		originalTrack.bigramRegions.do{ |region,regionIndex|
			var startBPD = region.startBPD;
			var endBPD = region.endBPD;
			bigramTracks.at(newTrackIndex).addRegion(startBPD,endBPD);
			bigramTracks.at(newTrackIndex).bigramRegions.at(regionIndex).bpdList.postln;
			region.notes.do { |note|
				note.print;
				bigramTracks.at(newTrackIndex).bigramRegions.at(regionIndex).putNote(note.copy);
			}
		};

		^name
	}

	// this will be called probably from inside BigramTrack
	/*	setTrackName { |newName|
	// 1. search the position of the track
	var pos = bigramTrackNames
	}*/

	removeTrack { |trackName|
		// return the index of the deleted track, or nil if not
		var ans;
		var pos = bigramTrackNames.indexOf(trackName);

		"remove ".post;pos.postln;

		if (pos.isNil.not) {
			bigramTracks.removeAt(pos);
			bigramTrackNames.removeAt(pos);

			numTracks = numTracks - 1;

			ans = pos;
		} {
			("track " ++ trackName ++ " does not exist").warn;
			ans = nil;
		}

		^ans;
	}

	removeAllTracks {
		//reset values
		bigramTracks = List.new;
		bigramTrackNames = List.new;
		numTracks = 0;
		numTotalTracks = 0;
	}




	///////////////////////// REPRODUCTION CONTROL /////////////////////////

	play { |startPulseIndex,loopPulseIndex,loop|
		var pBinds = Array.new(bigramTracks.size);
		var repeats;
		var startBPD = this.getBPDfromPulseIndex(startPulseIndex);
		var endBPD;

		loopPulseIndex = loopPulseIndex ? 0;

		if (loop) {
			var loopBPD = this.getBPDfromPulseIndex(loopPulseIndex);
			if (loopBPD.after(startBPD)) {
				endBPD = loopBPD;
			} {
				endBPD = nil;
			};
			repeats = inf;
		} {
			endBPD = nil;
			repeats = 1;
		};


		bigramTracks.do { |track|
			pBinds.add(track.createPatterns(startBPD,endBPD));
		};
		// TODO: pasar al window el número de pulses y la velocidad
		// para que vaya moviéndose el puntero

		eventStreamPlayer = Ppar(pBinds,repeats:repeats).play;

		// create observer, remove last if existing
		if (eventStreamPlayerController.isNil.not) {
			eventStreamPlayerController.remove;
		};
		eventStreamPlayerController = SimpleController(eventStreamPlayer);
		eventStreamPlayerController.put(\userStopped,{
			"usersttopped".postln;
			{bigramEditorWindow.playButton.value_(0)}.defer;
		});
		eventStreamPlayerController.put(\stopped,{
			{bigramEditorWindow.playButton.value_(0)}.defer;
		});
	}

	pause {
		eventStreamPlayer.pause;
	}

	record {

	}

	setMetronom { |bool|

	}

}
