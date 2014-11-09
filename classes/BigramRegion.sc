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
// along with this program. If not, see <http://www.gnu.org/licenses/>
//
////////////////////////////////////////////////////////////////////////////

BigramRegion {

	var <bigramTrack; // the track where the region is
	var <bigramEditor; // the main editor

	var <>startBPD;
	var <>endBPD;
	var <>numPulses;

	var <startPulseIndex;
	var <endPulseIndex;

	var <>selected;

	var <notes;
	var <eventStreamPlayer;

	var <barsFromPulses;
	var <divisionsFromPulses;
	var <bpdList;
	var <pulsesFromPulseIndex;


	*new { |track, start, end|
		^super.new.init(track,start,end);
	}

	init { |track, start, end|
		bigramTrack = track;
		bigramEditor = track.bigramEditor;

		startBPD = start;
		endBPD = end;

		notes = List.new;
		selected = true;

		this.configure;

	}

	configure {
		// "config".postln;
		numPulses = bigramEditor.getPulseIndexFromBPD(endBPD) - bigramEditor.getPulseIndexFromBPD(startBPD) + 1;


		////////
		startPulseIndex = bigramEditor.getPulseIndexFromBPD(startBPD);
		endPulseIndex = bigramEditor.getPulseIndexFromBPD(endBPD);

		barsFromPulses = bigramEditor.barsFromPulses[startPulseIndex..endPulseIndex];


		divisionsFromPulses = Array.new(numPulses);
		numPulses.do { |pulse|
			var bar = barsFromPulses.at(pulse);
			var divisionsAtBar = bigramEditor.measuresFromBars.at(bar).division;
			divisionsFromPulses.add(divisionsAtBar);
		};

		pulsesFromPulseIndex = List.new;
		{
			var lastBar;
			var pulse;
			barsFromPulses.do { |bar,pulseIndex|
				if (pulseIndex == 0) {
					pulse = startBPD.pulse;
				} {
					if (bar == lastBar) {
						pulse = pulse + 1;
					} {
						pulse = 0;
					}
				};
				pulsesFromPulseIndex.add(pulse);
				lastBar = bar;
			};
		}.value();

		bpdList = List.new;
		divisionsFromPulses.do{ |divisions,pulseIndex|
			divisions.do { |d|
				var bar = barsFromPulses.at(pulseIndex);
				bpdList.add(BPD.new(bar,pulsesFromPulseIndex[pulseIndex],d))
			}
		};
	}

	setStartBPD { |bpd|
		startBPD = bpd;
		this.configure;
		//remove notes outside range
		notes.do{ |note,noteIndex|
			if (note.bpd.before(startBPD)) {
				this.removeNoteAt(noteIndex);
			};
		}
	}

	setEndBPD { |bpd|
		var index, nextBpd;
		endBPD = bpd;
		this.configure;

		index = bigramEditor.getPulseIndexFromBPD(endBPD);
		nextBpd = bigramEditor.getBPDfromPulseIndex(index+1);
		//remove notes outside range
		notes.do{ |note,noteIndex|
			if (nextBpd.isNil.not) {
				if (note.bpd.afterEqual(nextBpd)) {
					this.removeNoteAt(noteIndex);
				};
			} {
				// "LAST INDEX!!!!!!!!!!!!!!!!!!!!!!!".postln;
			}
		}
	}

	///////////////////////// NOTE MANAGING /////////////////////////

	putNote{ |note, save=true|
		notes.add(note);
		if (save) {
			"save put note".postln;
			bigramEditor.bigramEditorWindow.saveTmp;
		};
	}

	removeNoteAt{ |index|
		notes.removeAt(index);
		"save remove note".postln;
		bigramEditor.bigramEditorWindow.saveTmp;
	}

	getNoteAt{ |index|
		^notes.at(index)
	}

	getAllNotes{
		^notes.asArray;
	}

	removeAllNotes {
		notes = List.new;
	}

	getSelectedNotes {
		^notes.select(_.isSelected)
	}

	moveSelectedNotes { |direction,value,save=true|

		switch(direction)
		{\up} {
			notes.do{ |note|
				if (note.isSelected) {
					note.movePitch(value)
				}
			}
		}
		{\down} {
			notes.do{ |note|
				if (note.isSelected) {
					note.movePitch(value.neg)
				}
			}
		}
		{\left} {
			notes.do { |note|
				if (note.isSelected) {
					var bpd = note.bpd;
					// var index = bpdList.indexOf(bpd).postln;
					var index, indexBefore;
					bpdList.do{|aBpd,i|
						if (aBpd.equal(bpd)) {index = i}
					};
					indexBefore = (index-value).clip(0,bpdList.size-1);
					note.bpd_(bpdList.at(indexBefore));
				}
			}
		}
		{\right} {
			notes.do { |note|
				if (note.isSelected) {
					var bpd = note.bpd;
					var index, indexAfter;
					// var index = bpdList.indexOf(bpd);
					bpdList.do{|aBpd,i|
						if (aBpd.equal(bpd)) {index = i}
					};
					indexAfter = (index+value).clip(0,bpdList.size-1);
					note.bpd_(bpdList.at(indexAfter));
				}
			}

		}
		;
		if (save) {
			"save move selected notes".postln;
			bigramEditor.bigramEditorWindow.saveTmp;
		}
	}

	duplicateSelectedNotes {
		var newNotes = List.new;
		notes.do { |note|
			if (note.isSelected) {
				//create copy
				var newNote = note.copy.isSelected_(false);
				newNotes.add(newNote);
				this.putNote(newNote,save:true);
				note.isSelected_(false);
			}
		};
		newNotes.do(_.isSelected_(true));
	}

	removeSelectedNotes {
		// we do it backwards because, when removing a note, the indexes go back
		var noteIndices = notes.size;
		if (notes.size > 0) {
			(noteIndices-1..0).do{ |noteIndex|
				var note = notes.at(noteIndex);

				if (note.isSelected) {
					this.removeNoteAt(noteIndex);
				}
			}
		}
	}

	hasNote { |note|
		var noteIndex = notes.collect{|n| n.sameAs(note)}.indexOf(true);
		^noteIndex;
	}

	selectAllNotes {
		notes.do(_.isSelected_(true))
	}

	deselectAllNotes {
		notes.do(_.isSelected_(false))
	}


	getSubdivisionIndex { |note|
		var index;
		/*		"bpdList".postln;
		bpdList.do(_.print);
		note.bpd.print; ////////////////////////////// <---*/
		bpdList.do{ |bpd,i|
			if (bpd.equal(note.bpd)) {index = i}
		};
		^index;
	}

	// TIME

	getNotesAtBpd { |bpd|
		var result = List.new;
		notes.do{ |note|
			if (note.atPosition(bpd)) {result.add(note)};
		};
		^result;
	}

	getNotesFromBpd { |bpd|
		var result = List.new;
		notes.do{ |note|
			if (note.isAfterEqual(bpd)) {result.add(note)};
		};
		^result;
	}

	getNotesUntilBpd { |bpd|
		var result = List.new;
		notes.do{ |note|
			if (note.isBeforeEqual(bpd)) {result.add(note)};
		};
		^result;
	}

	getNotesBetweenBpd { |bpd1, bpd2|
		var first, last;
		var notesFrom, notesUntil;
		// see which one is before
		if (bpd1.after(bpd2)) {
			first = bpd2;
			last = bpd1;
		} {
			first = bpd1;
			last = bpd2;
		};
		// get notes
		notesFrom = this.getNotesFromBpd(first);
		notesUntil = this.getNotesUntilBpd(last);

		^notesFrom.select{|note|notesUntil.indexOf(note).isNil.not};

	}

	// pitch

	getNotesAtPitch { |pitdch|
		var result = List.new;
		notes.do{ |note|
			if (note.atPitch(pitdch)) {result.add(note)};
		};
		^result;
	}

	getNotesFromPitch { |pitch|
		var result = List.new;
		notes.do{ |note|
			if (note.isHigherEqual(pitch)) {result.add(note)};
		};
		^result;
	}

	getNotesUntilPitch { |pitch|
		var result = List.new;
		notes.do{ |note|
			if (note.isLowerEqual(pitch)) {result.add(note)};
		};
		^result;
	}

	getNotesBetweenPitch { |pitch1,pitch2|
		var low,high;
		var notesFrom,notesUntil;
		// comparison
		if (pitch1.higher(pitch2)) {
			low = pitch2;
			high = pitch1;
		} {
			low = pitch1;
			high = pitch2;
		};
		// get notes
		notesFrom = this.getNotesFromPitch(low);
		notesUntil = this.getNotesUntilPitch(high);

		^notesFrom.select{|note|notesUntil.indexOf(note).isNil.not};
	}

	// BOTH

	getNotesBetween { |note1, note2|
		var bpd = this.getNotesBetweenBpd(note1.bpd,note2.bpd);
		var pitch = this.getNotesBetweenPitch(note1.pitch, note2.pitch);

		^bpd.select{|note| pitch.indexOf(note).isNil.not};

	}

	////// PLAY

	// create patterns for inside regionWindow reproduction: index 0 at region start
	createPatterns {
		var degrees = List.new;
		var subdivisionIndices = List.new;

		var orderedDegrees;
		var orderedSubdivisionIndices;
		var orderedIndex;

		var durations = List.new;
		var tempos = List.new;
		var barDivisions = List.new;
		var amps = List.new;


		//// order notes by timestamp (subdivisionIndex)

		notes.do { |note|
			degrees.add(note.pitch.absPitch);
			subdivisionIndices.add(this.getSubdivisionIndex(note));
			tempos.add(bigramEditor.tempos.at(bigramEditor.getPulseIndexFromBPD(note.bpd)));
			// num of divisions of the note's bar
			barDivisions.add(bigramEditor.measuresFromBars.at(note.bpd.bar).division);
			amps.add(note.midiAmp);
		};

		orderedSubdivisionIndices = subdivisionIndices[subdivisionIndices.order];
		orderedDegrees = degrees[subdivisionIndices.order];
		tempos = tempos[subdivisionIndices.order];
		barDivisions = barDivisions[subdivisionIndices.order];
		amps = amps[subdivisionIndices.order];


		////translate the timestamps to the dur format

		// if the first element is not at time 0, add an extra 0 with a rest note
		if (orderedSubdivisionIndices.first != 0 ) {
			orderedSubdivisionIndices = orderedSubdivisionIndices.addFirst(0);
			// in that case, add the rest note
			orderedDegrees = orderedDegrees.addFirst(\rest);
		};

		// add an extra value in order to perform the following substraction
		orderedSubdivisionIndices = orderedSubdivisionIndices.add(orderedSubdivisionIndices.last+1);
		// TODO: with that we got a dur of 1 in last note;
		//       it should be adjusted p.e. to the bigram total duration


		orderedIndex = 0;
		(orderedSubdivisionIndices.size-1).do { |i|

			var delta = orderedSubdivisionIndices[i+1]-orderedSubdivisionIndices[i];


			if (delta > 0) {
				// sequential notes

				durations.add(delta);
				orderedIndex = orderedIndex + 1;
			} {
				// simultaneous notes:
				// put all orderedDegrees notes inside an array
				// and don't add the 0 to the duration array

				var simultaneousNote = orderedDegrees.at(orderedIndex+1);
				orderedDegrees.removeAt(orderedIndex+1);
				orderedDegrees.put(orderedIndex,[orderedDegrees.at(orderedIndex),simultaneousNote].flat);
			}
		};

		// adjust durations to real tempo
		// dur 1 is equal to quarter notes at 60bpm...
		durations = durations / barDivisions / (tempos/60);

		[orderedDegrees.asArray,durations.asArray].postln;

		// ^[orderedDegrees.asArray,durations.asArray];

		// midiout version
		^Pbind(
			\midinote, Pseq(orderedDegrees.asArray - 3, 1), // bigram main line is A, but is internally coded as 0 (C)
			\dur, Pseq(durations.asArray, 1),
			\amp, Pseq(amps.asArray),
			\type, \midi,
			\midiout,bigramEditor.bigramEditorWindow.midiOut,
			\chan,bigramTrack.midiChannel
		);

	}

	play {
		var degrees, durations;

		#degrees,durations = this.createPatterns;

		// we use eventStreamPlayer to monitor from outside if it's playing
		eventStreamPlayer=Pbind(
			\midinote, Pseq(degrees - 3, 1), // bigram main line is A, but is internally coded as 0 (C)
			\dur, Pseq(durations, 1),
		).play;


	}






}
