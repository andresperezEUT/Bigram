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
	var <>tempos; // a different Integer for each pulse
	var <>defaultTempo = 120; //in bpm

	var <>measures; // a different PD value for each bar
	var <>defaultMeasure; // PD(4,2), initialize in init

	var <>measuresFromBars; // bars:[PD,PD...]
	var <>numBars;

	var <defaultNumPulses = 0; // total number of pulses

	var <>barsFromPulses; // list with the bar number for each pulse

	var <numOctaves = 8;

	var <pattern;
	var <eventStreamPlayer;
	var <eventStreamPlayerController;
	var <bigramEditorWindow;

	var <>tmpFilePath;
	var <>tmpVersion;
	var <>tmpMaxVersion;


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

		tmpFilePath = Platform.defaultTempDir +/+ "bigram_" ++ Date.getDate.stamp;
		tmpVersion = nil;
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

		var bar, pulse, division;

		// "GET BPD FROM PULSE INDEX".postln;
		// ["pulseIndex",pulseIndex].postln;

		if ( pulseIndex < barsFromPulses.size) {
			bar = this.getBarNumber(pulseIndex);
			// ["bar",bar].postln;
			// ["indices of bar",barsFromPulses.indicesOfEqual(bar)].postln;
			barsFromPulses.indicesOfEqual(bar).do { |pulseNum,i|
				if (pulseNum == pulseIndex) {
					pulse = i;
				};
			};
			// ["index of pulse",barsFromPulses.indicesOfEqual(bar).indexOf(pulseIndex)].postln;
			// pulse = barsFromPulses.indicesOfEqual(bar).indexOf(pulseIndex);
			// ["pulse",pulse].postln;
			division = 0;

			^BPD.new(bar,pulse,division);
		} {
			^nil;
		}
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

	addBars { |bars,measure,tempo,save=true|
		var lastBarNumber;
		// append to measuresFromBars
		numBars = numBars + bars;
		bars.do{
			measuresFromBars.add(measure)
		};
		//append to barsFromPulses
		lastBarNumber = barsFromPulses.last ? -1;
		bars.do { |bar|
			var barNumber = lastBarNumber + 1 + bar;
			measure.pulse.do {
				barsFromPulses.add(barNumber);
			};
		};
		//append to tempos
		bars.do{
			measure.pulse.do{
				tempos.add(tempo ? defaultTempo);
			};
		};

		// change canvas width
		bigramEditorWindow.recalculateBigramCanvasWidth;
		// refresh view
		// bigramEditorWindow.updateView;

		if (save) {
			"save add bars".postln;
			bigramEditorWindow.saveTmp;
		};
	}





	///////////////////////// TRACK MANAGING /////////////////////////


	addTrack { |save=true|
		var name = numTotalTracks.asSymbol;
		bigramTracks.add(BigramTrack.new(this,name));
		bigramTrackNames.add(numTotalTracks.asSymbol);

		numTracks = numTracks + 1;
		numTotalTracks = numTotalTracks + 1;

		if (save) {
			"save add track".postln;
			bigramEditorWindow.saveTmp;
		}

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

			bigramEditorWindow.saveTmp;
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


	/////////////////////// IMPORT MIDI //////////////////////////////7

	/////////// IMPORT MIDI /////////////////

	importMidiFile { |path|
		var notes = List.new;
		var fracs;

		var pulse;
		var division;

		var m = SimpleMIDIFile.read(path);
		var tempo = m.tempo;
		var measure = m.timeSignatureAsArray;
		var numTracks = m.tracks;
		var noteTracks = List.new; // tracks with noteOn events
		var channelsInTracks = List.new; // channel for each track
		var fracsInTracks = List.new; //list of tempo fracs for each track
		var programsInTracks = List.new; // midi program for each track

		var maxPulse;
		var numBars;

		// get tracks with notes and channels
		numTracks.do { |track|
			var hasNoteOn = false;
			var channel;
			m.midiTrackEvents(track).do { |e|
				if (e[2]=='noteOn') {
					channel = e[3];
					hasNoteOn = true;
				};
			};
			if (hasNoteOn) {
				noteTracks.add(track);
				channelsInTracks.add(channel);
			};
		};
		"noteTracks".postln;
		noteTracks.postln;
		"channelsInTracks".postln;
		channelsInTracks.postln;

		// get programs
		noteTracks.do { |track|
			var program;
			m.midiTrackEvents(track).do { |e|
				if (e[2]=='program') {
					program = e[4];
				};
			};
			// default program
			if (program.isNil) {program=0};
			programsInTracks.add(program);
		};

		//get note list
		noteTracks.do { |track|
			var frac = List.new;
			var n = List.new;
			m.midiTrackEvents(track).do{ |e|
				//[ trackNumber, absTime, type, channel, val1:pitch, val2 :amp]
				if (e[2]=='noteOn' and:{e[5]!=0}) { // avoid note offs
					n.add([m.beatAtTime(e[1]),e[4],e[5]]);
					frac.add(m.beatAtTime(e[1]).frac);
				}
			};

			notes.add(n.asArray);
			// frac = frac.select(_>0).reciprocal.postln;
			fracsInTracks.add(frac);
		};

		// get most used intervals
		{
			var half = 0;
			var third = 0;
			var quarter = 0;

			var histograms = List.new;
			fracsInTracks.do{ |frac, track|
				var size = notes[track].size;
				var h = List.new;
				["size",size,track].postln;
				frac.histo(100,0,1).do{|v,i|if(v>(size/10)){h.add(i)}};
				histograms.add(h);
			};

			histograms.do{ |histo,track|
				("histo"++track).postln;
				histo.postln;
				histo.do { |div,i|
					div.postln;
					switch(div)
					{0} {}
					// 2 subdivisions
					{50} {half = half+1}
					// 3 subdivisions
					{33} {third = third + 1}
					{66} {third = third + 1}
					// 4 subdivisions
					{25} {quarter = quarter + 1}
					{75} {quarter = quarter + 1}
					// TODO: complete!
					{};
				}
			};

			// set pulse and division
			pulse = measure[0];
			if ( quarter >= third ) {
				division = 4;
			} {
				division = 3;
			};
		}.();

		// get number of pulses
		maxPulse=0;
		notes.do { |noteList| // note[beat,pitch]
			var pulse = noteList.last[0].round;
			if ( pulse > maxPulse) {
				maxPulse = pulse;
			}
		};
		maxPulse = maxPulse + 1; // add some extra duration

		// ADD TO BIGRAM STRUCTURE

		// add tracks
		noteTracks.do { |track,i|
			var a = ["set track",track,i].postln;
			var trackName = bigramEditorWindow.addTrack(save:false); // from window to put gui controls
			var trackIndex = bigramTrackNames.indexOf(trackName);
			var trackRef = bigramTracks.at(trackIndex);
			// set midi program
			("set program for track"++track).postln;
			programsInTracks.postln;
			programsInTracks[i].postln;
			bigramEditorWindow.setMidiProgram(trackName,programsInTracks[i],save:false);
			//set channel
			trackRef.setChannel(channelsInTracks[i]);
		};

		// add bars
		pulse = measure[0];
		numBars = (maxPulse / pulse).ceil;
		this.addBars(numBars,measure:PD.new(pulse,division),tempo:tempo,save:false);


		// add regions: one for each track
		bigramTracks.do { |track|
			track.addRegion(BPD.new(0,0,0).print,this.getBPDfromPulseIndex(maxPulse),save:false);
		};


		// set midi instruments: todo!

		// put notes
		notes.do { |noteList,i|
			noteList.do { |note|
				var track = bigramTracks[i];
				// temporal info
				var noteBeat = note[0];
				var roundNoteBeat = noteBeat.round(1/division);
				var noteBar = (roundNoteBeat / pulse).floor;
				var notePulse = (roundNoteBeat % pulse).floor;
				var noteDivision = roundNoteBeat.frac * division;
				// pitch info
				var absPitch = note[1];
				// amp info
				var amp = note[2];

				var pitch = BPitch.newAbsPitch(absPitch+3); // after is taken away
				var bpd = BPD.new(noteBar,notePulse,noteDivision);

				var newNote = BigramNote.new(pitch,bpd).midiAmp_(amp);

				["newNote"].postln;
				newNote.print;
				track.bigramRegions.first.putNote(newNote,save:false);
			};
		};

		// savetmp bug fix
		// "--------------\n--------------\n--------------".postln;
		// barsFromPulses.first.class.postln;
		barsFromPulses = barsFromPulses.collect(_.asInt);
		// barsFromPulses.first.class.postln;
		// "--------------\n--------------\n--------------".postln;

		bigramEditorWindow.saveTmp;



	}

}
