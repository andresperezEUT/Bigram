Bigram {

	var <notes;
	var <eventStreamPlayer;

	*new {
		^super.new.init;
	}

	init {
		notes = List.new;
	}


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
		^notes
	}

	removeAllNotes {
		notes = List.new;
	}

	getNotesAt { |beat, relativePos|
		var result = List.new;
		notes.do{ |note|
			if (note.atPosition(beat,relativePos)) {result.add(note)};
		};
		^result;
	}

	getNotesFrom { |beat, relativePos|
		var result = List.new;
		notes.do{ |note|
			if (note.isAfter(beat,relativePos)) {result.add(note)};
		};
		^result;
	}

	getNotesUntil { |beat, relativePos|
		var result = List.new;
		notes.do{ |note|
			if (note.isBefore(beat,relativePos)) {result.add(note)};
		};
		^result;
	}

	getNotesInTimeRange { |beat1, relativePos1, beat2, relativePos2|
		var result = List.new;

		// if 2 < 1, interchange variables
		if (BigramNote.getAbsPos(beat1,relativePos1) > BigramNote.getAbsPos(beat2,relativePos2)) {
			var auxBeat = beat1;
			var auxRelativePos = relativePos1;

			beat1 = beat2;
			relativePos1 = relativePos2;

			beat2 = auxBeat;
			relativePos2 = auxRelativePos;
		};

		notes.do{ |note|
			if (note.isBefore(beat2,relativePos2) and:{note.isAfter(beat1,relativePos1)}) {result.add(note)};
		};
		^result;
	}

	getNotesInPitchRange { |height1, pitchClass1, height2, pitchClass2|
		var result = List.new;

		// if 2 < 1, interchange variables
		if (BigramNote.getAbsPitch(height1,pitchClass1) > BigramNote.getAbsPitch(height2,pitchClass2)) {
			var auxHeight = height1;
			var auxClass = pitchClass1;

			height1 = height2;
			pitchClass1 = pitchClass2;

			height2 = auxHeight;
			pitchClass2 = auxClass;
		};

		notes.do{ |note|
			if (note.isLower(height2,pitchClass2) and:{note.isHigher(height1,pitchClass1)}) {result.add(note)}
		};
		^result
	}

	getNotesInRange {  |beat1, relativePos1, beat2, relativePos2, height1, pitchClass1, height2, pitchClass2|
		var inTimeRange = List.new;
		var result = List.new;

		// if 2 < 1, interchange variables
		if (BigramNote.getAbsPos(beat1,relativePos1) > BigramNote.getAbsPos(beat2,relativePos2)) {
			var auxBeat = beat1;
			var auxRelativePos = relativePos1;

			beat1 = beat2;
			relativePos1 = relativePos2;

			beat2 = auxBeat;
			relativePos2 = auxRelativePos;
		};
		if (BigramNote.getAbsPitch(height1,pitchClass1) > BigramNote.getAbsPitch(height2,pitchClass2)) {
			var auxHeight = height1;
			var auxClass = pitchClass1;

			height1 = height2;
			pitchClass1 = pitchClass2;

			height2 = auxHeight;
			pitchClass2 = auxClass;
		};


		//get notes within temporal range
		notes.do{ |note|
			if (note.isBefore(beat2,relativePos2) and:{note.isAfter(beat1,relativePos1)}) {inTimeRange.add(note)};
		};
		//now select from these notes the ones in the pitch range
		inTimeRange.do{ |note|
			if (note.isLower(height2,pitchClass2) and:{note.isHigher(height1,pitchClass1)}) {result.add(note)}
		};
		^result;
	}

	noteIndex { |aNote|
		var result = List.new;
		var allNotes = this.getAllNotes;
		allNotes.do{ |note,i|
			if (note.sameAs(aNote)) {
				^i;
			}
		};
		^nil;
	}

	getSelectedNotes {
		^notes.select(_.isSelected);
	}

	getDeselectedNotes {
		^notes.reject(_.isSelected);
	}

	selectNotes { |notes|
		notes.do(_.isSelected_(true))
	}

	deselectNotes { |notes|
		notes.do(_.isSelected_(false))
	}

	selectAllNotes {
		notes.do(_.isSelected_(true))
	}

	deselectAllNotes {
		notes.do(_.isSelected_(false))
	}

	moveSelectedNotes { |direction, amount|
		notes.do{ |note|
			if (note.isSelected) {
				switch (direction)
				{\up} {note.moveUp(amount)}
				{\down} {note.moveDown(amount)}
				{\left} {note.moveLeft(amount)}
				{\right} {note.moveRight(amount)};
			}
		}
	}

	removeSelectedNotes {
		// go backwards to avoid indexing problems with removed notes
		var length = notes.size;
		(length-1..0).do { |i|
			if (notes[i].isSelected) {
				this.removeNoteAt(i)
			}
		}
	}


	////////////// SOUND //////////////////

	play {
		var degrees, durations;

		#degrees,durations = this.createPatterns;

		// we use eventStreamPlayer to monitor from outside if it's playing
		eventStreamPlayer=Pbind(
			\midinote, Pseq(degrees - 3, 1), // bigram main line is A, but is internally coded as 0 (C)
			\dur, Pseq(durations, 1),
		).play;


	}


	// TODO: optimize by creating arrays instead of lists

	createPatterns {
		var durations = List.new;

		var degrees = List.new;
		var times = List.new;
		var orderedTimes = List.new;
		var orderedDegrees = List.new;
		var orderedIndex;

		// order notes by timestamp
		notes.do { |note|
			times.add(note.absPos);
			degrees.add(note.absPitch);
		};
		orderedTimes = times[times.order];
		orderedDegrees = degrees[times.order];


		//translate the timestamps to the dur format

		// if the first element is not at time 0, add an extra 0 with a rest note
		if (orderedTimes.first != 0 ) {
			orderedTimes = orderedTimes.addFirst(0);
			// in that case, add the rest note
			orderedDegrees = orderedDegrees.addFirst(\rest);
		};

		// add an extra value in order to perform the following substraction
		orderedTimes = orderedTimes.add(orderedTimes.last+1);
		// TODO: with that we got a dur of 1 in last note;
		//       it should be adjusted p.e. to the bigram total duration


		orderedIndex = 0;
		(orderedTimes.size-1).do { |i|

			var delta = orderedTimes[i+1]-orderedTimes[i];


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

		^[orderedDegrees.asArray,durations.asArray]
	}


	///// SAVE AND LOAD //////

	saveBigram { |fileName = "bigram"|

		var file = File.new(fileName,"w");

		notes.do { |note|
			file.write(note.height.asString);
			file.write(",");
			file.write(note.pitchClass.asString);
			file.write(",");
			file.write(note.beat.asString);
			file.write(",");
			file.write(note.relativePos.asString);
			file.write("\n");
		};

		file.close;
	}

	loadBigram { |fileName|

		// load content into file var
		var file = CSVFileReader.read(fileName);

		// remove all notes
		this.removeAllNotes;

		// place new notes

		file.do { |line|
			var height = line[0].asFloat;
			var pitchClass = line[1].asFloat;
			var beat = line[2].asFloat;
			var relativePos = line[3].asFloat;

			this.putNote(BigramNote(height,pitchClass,beat,relativePos));
		};
	}
}


BigramNote {
	var <>height, <pitchClass, <type;
	var <>beat, <>relativePos;
	var <>isSelected;

	var <duration; // <--- ??

	*new { |height, pitchClass, beat, relativePos|
		^super.new.init(height,pitchClass,beat,relativePos)
	}

	init { |myHeight, myPitchClass, myBeat, myRelativePos|
		height = myHeight ? 1;
		pitchClass = myPitchClass ? 0;
		beat = myBeat ? 0;
		relativePos = myRelativePos ? 1;

		type = this.getType;
		isSelected = false;

	}

	getType {
		if (pitchClass.asInt.odd) {
			^\w;
		} {
			^\b;
		}
	}

	absPitch {
		^(height*12) + pitchClass;
	}

	absPos {
		^beat + relativePos;
	}

	*getAbsPitch { |height, pitchClass|
		^(height*12) + pitchClass;
	}

	*getAbsPos { |beat, relativePos|
		^beat + relativePos;
	}

	*getRelativePitch { |absPitch|
		var height, pitchClass;
		height = (absPitch / 12).floor;
		pitchClass = (absPitch % 12);
		^[height, pitchClass];
	}

	*getRelativePos { |absPos|
		var beat, relativePos;
		beat = absPos.floor;
		relativePos = absPos - beat;
		^[beat, relativePos]
	}


	moveUp { |semitones=1|
		var newPitchClass = pitchClass + semitones;
		if (newPitchClass > 11) {
			pitchClass = newPitchClass % 12;
			height = height + ((newPitchClass / 12).floor);
		} {
			pitchClass = newPitchClass;
		};
		type = this.getType;
	}

	moveDown { |semitones=1|
		var newPitchClass = pitchClass - semitones;
		if (newPitchClass < 0) {
			pitchClass = newPitchClass % 12;
			height = height - 1 - ((newPitchClass / 12).neg.floor)
		} {
			pitchClass = newPitchClass;
		};
		type = this.getType;
	}

	moveLeft { |amount=1|
		var pos, newPos;
		pos = beat + relativePos;
		newPos = pos - amount;
		if (newPos < 0) {newPos = 0};

		beat = newPos.floor;
		relativePos = newPos - beat;
	}

	moveRight { |amount=1|
		var pos, newPos;
		pos = beat + relativePos;
		newPos = pos + amount;

		beat = newPos.floor;
		relativePos = newPos - beat;

	}

	setClass { |newPitchClass|
		pitchClass = newPitchClass;
		type = this.getType;
	}

	////// note comparison


	atPosition { |myBeat,myRelativePos|
		if (BigramNote.getAbsPos(myBeat,myRelativePos) == this.absPos) {^true} {^false}
		// if (myBeat == beat and:{myRelativePos == relativePos}) {^true} {^false}
	}

	isAfter { |myBeat,myRelativePos|
		if (BigramNote.getAbsPos(myBeat,myRelativePos) <= this.absPos) {^true} {^false}
	}

	isBefore { |myBeat,myRelativePos|
		if (BigramNote.getAbsPos(myBeat,myRelativePos) >= this.absPos) {^true} {^false}
	}

	atPitch { |myHeight, myClass|
		if (BigramNote.getAbsPitch(myHeight,myClass) == this.absPitch) {^true} {^false}
	}

	isHigher { |myHeight, myClass|
		if (BigramNote.getAbsPitch(myHeight,myClass) <= this.absPitch) {^true} {^false}
	}

	isLower { |myHeight, myClass|
		if (BigramNote.getAbsPitch(myHeight,myClass) >= this.absPitch) {^true} {^false}
	}

	sameAs { |aNote|
		if (height == aNote.height and:{pitchClass == aNote.pitchClass and:{beat == aNote.beat and:{relativePos == aNote.relativePos}}}) {^true} {^false}

	}


	////// other
	print {
		[height,pitchClass,beat,relativePos].postln;
	}
}
