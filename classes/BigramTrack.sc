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
			if (note.bpd.afterEqual(nextBpd)) {
				this.removeNoteAt(noteIndex);
			};
		}
	}

	///////////////////////// NOTE MANAGING /////////////////////////

	putNote{ |note|
		notes.add(note)
	}

	removeNoteAt{ |index|
		notes.removeAt(index)
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

	moveSelectedNotes { |direction,value|

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
	}

	removeSelectedNotes {
		// we do it backwards because, when removing a note, the indexes go back
		var noteIndices = notes.size;
		(noteIndices-1..0).do{ |noteIndex|
			var note = notes.at(noteIndex);

			if (note.isSelected) {
				this.removeNoteAt(noteIndex);
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


BigramTrack {

	var <bigramEditor; // reference to the editor

	var <bigramRegions; // list containing all regions
	var <bigramRegionPositions; // same order as bigramRegions
	var <bigramRegionNames; // ""
	var <numTotalRegions = 0; // for naming purposes


	var <>selected; // whether track is selected for edition (visual)

	/// track controls
	var <>name; //track identifier
	var <record = 0;
	var <>muteAmp = 1;
	var <>solo = 0;
	var <>pan = 64;
	var <>volume = 127;
	var <>midiChannel; // auto-assigned: 0..15 except 9
	var <>midiProgram = 0; // user asigned; 0..127




	*new { |editor, newName|
		^super.new.init(editor).setName(newName).setChannel(newName);
	}

	init { |editor|

		bigramEditor = editor;
		bigramRegions = List.new;
		bigramRegionPositions = List.new;

		bigramRegionNames = List.new;


	}

	setName { |newName|
		name = newName.asSymbol;
	}

	setChannel { |newName|
		// midi channel will be the name (excluding 9)
		newName = newName.asInt;
		if (newName < 8 ) {
			midiChannel = newName;
		} {
			midiChannel = newName + 1;
		};
	}

	getRegionIndexByName { |regionName|
		^bigramRegionNames.indexOf(regionName);
	}





	///////////////////////// REGION MANAGING /////////////////////////

	addRegion {|startBPD, endBPD|
		bigramRegions.add(BigramRegion.new(this,startBPD,endBPD));
		bigramRegionPositions.add([startBPD,endBPD]);
		bigramRegionNames.add(numTotalRegions.asSymbol);
		numTotalRegions = numTotalRegions + 1;
		^(numTotalRegions-1).asSymbol;
	}

	removeRegion { |regionName|
		var pos = this.getRegionIndexByName(regionName);

		if (pos.isNil.not) {
			bigramRegions.removeAt(pos);
			bigramRegionPositions.removeAt(pos);
			bigramRegionNames.remove(regionName);
		} {

		}

	}

	// from is \init or \end
	resizeRegion { |regionName, from, newBPD |
		var pos = this.getRegionIndexByName(regionName);
		var region = bigramRegions.at(pos);

		if (from==\init) {
			if (newBPD.beforeEqual(region.endBPD)) {
				region.setStartBPD(newBPD);
			}
		} {
			if (newBPD.afterEqual(region.startBPD)) {
				region.setEndBPD(newBPD);
			}
		};
	}

	// dir is \left or \right
	moveRegion { |regionName, numDivisions, dir|
		var pos = this.getRegionIndexByName(regionName);
		var region = bigramRegions.at(pos);

		var startIndex = bigramEditor.getPulseIndexFromBPD(region.startBPD);
		var endIndex = bigramEditor.getPulseIndexFromBPD(region.endBPD);

		//first move towars bigger in order to not remove notes
		switch (dir)
		{\left} {
			// convert pulses into divisions number for moveSelectedNotes method
			var beforeStartIndex = startIndex - numDivisions;
			var bar = bigramEditor.barsFromPulses.at(beforeStartIndex);
			var measure = bigramEditor.measuresFromBars.at(bar);
			var divisionsPerPulse = measure.division;

			region.setStartBPD(bigramEditor.getBPDfromPulseIndex(startIndex-numDivisions));
			region.selectAllNotes;
			region.moveSelectedNotes(\left,numDivisions*divisionsPerPulse);
			region.deselectAllNotes;
			region.setEndBPD(bigramEditor.getBPDfromPulseIndex(endIndex-numDivisions));
		}
		{\right} {
			var afterEndIndex = endIndex + numDivisions;
			var bar = bigramEditor.barsFromPulses.at(afterEndIndex);
			var measure = bigramEditor.measuresFromBars.at(bar);
			var divisionsPerPulse = measure.division;

			region.setEndBPD(bigramEditor.getBPDfromPulseIndex(endIndex+numDivisions));
			region.selectAllNotes;
			region.moveSelectedNotes(\right,numDivisions*divisionsPerPulse);
			region.deselectAllNotes;
			region.setStartBPD(bigramEditor.getBPDfromPulseIndex(startIndex+numDivisions));
		}
	}

	duplicateRegion { |regionName|
		var say=("DUPLICATE REGION" ++ regionName).postln;
		var pos = this.getRegionIndexByName(regionName);
		var region = bigramRegions.at(pos);

		var newRegionName = this.addRegion(region.startBPD,region.endBPD);
		var newRegion = bigramRegions.at(bigramRegionNames.indexOf(newRegionName));

		// avoid multi-duplication
		newRegion.selected_(false);

		region.notes.do{|note|
			newRegion.putNote(note.copy);
		};

	}

	deselectAllRegions {
		bigramRegions.do(_.selected_(false));
	}

	groupRegions { |region1, region2|

		var index1 = bigramRegions.indexOf(region1);
		var regionName1 = bigramRegionNames.at(index1);
		var index2 = bigramRegions.indexOf(region2);
		var regionName2 = bigramRegionNames.at(index2);
		/*		var index1 = this.getRegionIndexByName(regionName1);
		var region1 = bigramRegions.at(index1);
		var index2 = this.getRegionIndexByName(regionName2);
		var region2 = bigramRegions.at(index2);*/

		// region1 is the one before
		if (region2.startBPD.before(region1.startBPD)) {
			var aux = region2;
			region2 = region1;
			region1 = aux;
		};

		// check if they are consecutive
		{
			var end1index = bigramEditor.getPulseIndexFromBPD(region1.endBPD);
			var start2index = bigramEditor.getPulseIndexFromBPD(region2.startBPD);

			// only in same track
			if (region1.bigramTrack == region2.bigramTrack) {
				if ((start2index - end1index) == 1 ) {
					// 1. resize region1
					this.resizeRegion(regionName1,\end,region2.endBPD);
					// 2. copy contents of region2 into region1
					region2.notes.do{ |note|
						region1.putNote(note.copy);
					};
					// 3. remove region2
					this.removeRegion(regionName2);
				} {
					"not consecutive".postln;
				}
			}
		}.();

	}

	splitRegion { |regionName, bpd|
		var regionIndex = this.getRegionIndexByName(regionName);
		var region = bigramRegions.at(regionIndex);

		// check if bpd is in between start and end region
		if (bpd.after(region.startBPD) and:{bpd.beforeEqual(region.endBPD)}) {
			var a=[bpd,region.startBPD,region.endBPD].postln;
			//1. create region2
			var regionName2 = this.addRegion(bpd,region.endBPD);
			var regionIndex2 = this.getRegionIndexByName(regionName2);
			var region2 = bigramRegions.at(regionIndex2);
			//2. copy notes after that
			region.getNotesFromBpd(bpd).do { |note|
				region2.putNote(note.copy);
			};
			//3. just in case, remove notes after from region1
			{
				var index = bigramEditor.getPulseIndexFromBPD(bpd);
				var newBpd = bigramEditor.getBPDfromPulseIndex(index+1);
				"********".postln;
				bpd.print;
				region.notes.do{ |note|
					note.bpd.print;
					if (note.isBefore(bpd).postln) {
						note.isSelected_(false)
					} {
						note.isSelected_(true)
					}
				};
				region.removeSelectedNotes;
			}.();
			//4. resize region1
			{
				var index = bigramEditor.getPulseIndexFromBPD(bpd);
				var newBpd = bigramEditor.getBPDfromPulseIndex(index-1);
				this.resizeRegion(regionName,\end,newBpd);
			}.();
		} {
			"not in between".postln;
		}
	}

	getRegionAtBPD { |bpd|
		var ans;
		bpd.print;
		bigramRegions.do { |r,i|
			var afterEqual = bpd.afterEqual(r.startBPD);
			var beforeEqual = bpd.beforeEqual(r.endBPD);
			if (afterEqual and: {beforeEqual}) {
				ans = bigramRegionNames.at(i);
			}
		};
		^ans;
	}



	// create patterns for whole reproduction: index 0 at bpd[0,0,0]
	// returns a Pbind of the whole track
	createPatterns { |startBPD, endBPD|
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
		bigramRegions.do{ |region|
			var initSubdivision = bigramEditor.getSubdivisionIndexFromBPD(region.startBPD);
			region.notes.do { |note|
				degrees.add(note.pitch.absPitch);
				subdivisionIndices.add(region.getSubdivisionIndex(note) + initSubdivision);
				// tempo of the note's bar
				tempos.add(bigramEditor.tempos.at(bigramEditor.getPulseIndexFromBPD(note.bpd)));
				// num of divisions of the note's bar
				barDivisions.add(bigramEditor.measuresFromBars.at(note.bpd.bar).division);
				amps.add(note.midiAmp);

			};
		};


		//// order in time

		orderedSubdivisionIndices = subdivisionIndices[subdivisionIndices.order];
		orderedDegrees = degrees[subdivisionIndices.order];
		tempos = tempos[subdivisionIndices.order];
		barDivisions = barDivisions[subdivisionIndices.order];
		amps = amps[subdivisionIndices.order];


		//// get only notes between desired limits
		if (startBPD.isNil.not) {
			if (endBPD.isNil.not) {

				// defined start and end
				var a = "DEFINED START AND END".postln;

				var startSubdivision, endSubdivision;
				var startIndex, endIndex;
				startSubdivision = bigramEditor.getSubdivisionIndexFromBPD(startBPD).postln;
				endSubdivision = bigramEditor.getSubdivisionIndexFromBPD(endBPD).postln;
				// interval [..) as required by array[a..b]
				startIndex = orderedSubdivisionIndices.indexOfGreaterThan(startSubdivision-1).postln;
				endIndex = orderedSubdivisionIndices.indexOfSmallerThan(endSubdivision).postln;

				[startIndex,endIndex].postln;

				if (startIndex.isNil or:{endIndex.isNil}) {
					// none of the notes is between: return array with only rest
					orderedSubdivisionIndices = [0];
					orderedDegrees = [\rest];
				} {
					// remove notes outside time region
					orderedSubdivisionIndices = orderedSubdivisionIndices[startIndex..endIndex];
					orderedDegrees = orderedDegrees[startIndex..endIndex];
					tempos = tempos[startIndex..endIndex];
					barDivisions = barDivisions[startIndex..endIndex];
					amps = amps[startIndex..endIndex];
					// translate positions to startPosition
					orderedSubdivisionIndices = orderedSubdivisionIndices - startSubdivision;
				}

			} {
				// only defined start

				var a = "DEFINE START".postln;

				var startSubdivision;
				var startIndex;
				startSubdivision = bigramEditor.getSubdivisionIndexFromBPD(startBPD);
				startIndex = orderedSubdivisionIndices.indexOfGreaterThan(startSubdivision-1);

				orderedSubdivisionIndices.postln;
				startSubdivision.postln;
				startIndex.postln;

				if (startIndex.isNil) {
					// note of the notes is after: return array with only rest
					orderedSubdivisionIndices = [0];
					orderedDegrees = [\rest];
				} {
					// remove notes outside time region
					orderedSubdivisionIndices = orderedSubdivisionIndices[startIndex..];
					orderedDegrees = orderedDegrees[startIndex..];
					tempos = tempos[startIndex..];
					barDivisions = barDivisions[startIndex..];
					amps = amps[startIndex..];
					// translate positions to startPosition
					orderedSubdivisionIndices = orderedSubdivisionIndices - startSubdivision;
				}
			}
		} {
			"NOT DEFINED".postln;
		};

		"pre".postln;
		orderedSubdivisionIndices.postln;

		["tempos",tempos].postln;
		["barDivisions",barDivisions].postln;

		////translate the timestamps to the dur format

		// if the first element is not at time 0, add an extra 0 with a rest note
		if (orderedSubdivisionIndices.first != 0 ) {
			orderedSubdivisionIndices = orderedSubdivisionIndices.addFirst(0);
			// in that case, add the rest note
			orderedDegrees = orderedDegrees.addFirst(\rest);
		};

		// add an extra value in order to perform the following substraction

		if (endBPD.isNil.not) {
			//adjust dur of last note to the divisions until end mark
			var a = ["endBPD",endBPD].postln;
			var startSubdivisionIndex = bigramEditor.getSubdivisionIndexFromBPD(startBPD);
			var endSubdivisionIndex = bigramEditor.getSubdivisionIndexFromBPD(endBPD);
			var as = ["endSubdivisionIndex",endSubdivisionIndex].postln;
			var lastSubdivisionIndex = orderedSubdivisionIndices.last;
			var diff = endSubdivisionIndex - startSubdivisionIndex - lastSubdivisionIndex;
			["indices",orderedSubdivisionIndices].postln;
			["diff",diff].postln;

			orderedSubdivisionIndices = orderedSubdivisionIndices.add(orderedSubdivisionIndices.last+diff);

		} {
			orderedSubdivisionIndices = orderedSubdivisionIndices.add(orderedSubdivisionIndices.last+1);
		};


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

		//create Pbind

		/*		^Pbind(
		\midinote, Pseq(orderedDegrees.asArray - 3, 1), // bigram main line is A , but is internally coded as 0 (C)
		\dur, Pseq(durations.asArray, 1),
		);*/

		// midiout version

		^Pbind(
			\midinote, Pseq(orderedDegrees.asArray - 3, 1), // bigram main line is A, but is internally coded as 0 (C)
			\dur, Pseq(durations.asArray, 1),
			\amp, Pseq(amps.asArray),
			\type, \midi,
			\midiout,bigramEditor.bigramEditorWindow.midiOut,
			\chan,midiChannel
			)
	}

/*	setMidiProgram { |programNumber|
		midiProgram = programNumber;
		// send CC
		bigramEditor.bigramEditorWindow.midiOut.program(midiChannel,programNumber);
	}

	setPanning { |midiValue|
		pan = midiValue;
		// sendCC
	}*/

}
