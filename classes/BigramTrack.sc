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
		^super.new.init(editor).setName(newName).setChannelAuto(newName);
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

	setChannelAuto { |newName|
		// midi channel will be the name (excluding 9)
		newName = newName.asInt;
		if (newName < 8 ) {
			midiChannel = newName;
		} {
			midiChannel = newName + 1;
		};
	}

	setChannel { |channel|
		midiChannel = channel;
	}

	getRegionIndexByName { |regionName|
		^bigramRegionNames.indexOf(regionName);
	}





	///////////////////////// REGION MANAGING /////////////////////////

	addRegion {|startBPD, endBPD, save=true|
		bigramRegions.add(BigramRegion.new(this,startBPD,endBPD));
		bigramRegionPositions.add([startBPD,endBPD]);
		bigramRegionNames.add(numTotalRegions.asSymbol);
		numTotalRegions = numTotalRegions + 1;

		if (save) {
			"save add region".postln;
			bigramEditor.bigramEditorWindow.saveTmp;
		};

		^(numTotalRegions-1).asSymbol;
	}

	removeRegion { |regionName,save=true|
		var pos = this.getRegionIndexByName(regionName);

		if (pos.isNil.not) {
			bigramRegions.removeAt(pos);
			bigramRegionPositions.removeAt(pos);
			bigramRegionNames.remove(regionName);
			if (save) {
				"save remove region".postln;
				bigramEditor.bigramEditorWindow.saveTmp;
			};
		} {

		}

	}

	// from is \init or \end
	resizeRegion { |regionName, from, newBPD, save=true|
		var pos = this.getRegionIndexByName(regionName);
		var region = bigramRegions.at(pos);

		if (from==\init) {
			if (newBPD.beforeEqual(region.endBPD)) {
				region.startBPD.print;
				newBPD.print;
				region.startBPD.equal(newBPD);
				if (region.startBPD.equal(newBPD).not) {
					if (save) {
					region.setStartBPD(newBPD); /////
						"save resize region".postln;
						bigramEditor.bigramEditorWindow.saveTmp;
					};
				};

			}
		} {
			if (newBPD.afterEqual(region.startBPD)) {
				if (region.endBPD.equal(newBPD).not) {
					region.setEndBPD(newBPD); //////
					if (save) {
						"save resize region".postln;
						bigramEditor.bigramEditorWindow.saveTmp;
					};
				};
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
			region.moveSelectedNotes(\left,numDivisions*divisionsPerPulse,save:false);
			region.deselectAllNotes;
			region.setEndBPD(bigramEditor.getBPDfromPulseIndex(endIndex-numDivisions));
		}
		{\right} {
			var afterEndIndex = endIndex + numDivisions;
			var bar = bigramEditor.barsFromPulses.at(afterEndIndex);
			var measure = bigramEditor.measuresFromBars.at(bar);
			var divisionsPerPulse = measure.division;

			var afterEndBPD = bigramEditor.getBPDfromPulseIndex(afterEndIndex);
			region.setEndBPD(afterEndBPD);
			region.selectAllNotes;
			region.moveSelectedNotes(\right,numDivisions*divisionsPerPulse,save:false);
			region.deselectAllNotes;
			region.setStartBPD(bigramEditor.getBPDfromPulseIndex(startIndex+numDivisions));
		};

		"save move region".postln;
		bigramEditor.bigramEditorWindow.saveTmp;
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
		// savetmp already provided in addRegion
/*		"save duplicate region".postln;
		bigramEditor.bigramEditorWindow.saveTmp;*/
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
					this.resizeRegion(regionName1,\end,region2.endBPD,save:false);
					// 2. copy contents of region2 into region1
					region2.notes.do{ |note|
						region1.putNote(note.copy);
					};
					// 3. remove region2
					this.removeRegion(regionName2,save:false);
					"save group regions".postln;
					bigramEditor.bigramEditorWindow.saveTmp;
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
			var regionName2 = this.addRegion(bpd,region.endBPD,save:false);
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

				region.notes.do{ |note|
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
				this.resizeRegion(regionName,\end,newBpd,save:false);
			}.();
			"save split regions".postln;
			bigramEditor.bigramEditorWindow.saveTmp;
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
				// amp
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

		~amps = amps;

		^Pbind(
			\midinote, Pseq(orderedDegrees.asArray - 3, 1), // bigram main line is A, but is internally coded as 0 (C)
			\dur, Pseq(durations.asArray, 1),
			\amp, Pseq(amps.linlin(0,127,0,1).asArray),
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
