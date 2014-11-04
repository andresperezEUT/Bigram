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

BigramRegionWindow : QWindow {
	//
	// 	var <bigramView;
	var insertButton, editButton;
	var buttonList;
	var playButton;
	var showBigramSubdivisionButton, showBigramSubdivision;

	var zoomPlusHButton, zoomMinusHButton;
	var zoomPlusVButton, zoomMinusVButton;

	var zoomSlider;

	// var beatSubdivisionMenu;
	//
	// 	var saveButton;
	// 	var loadButton;
	//
	var <margin = 50;
	var <elementSize = 30;
	var <bigramView;
	var <bigramView_height;
	var <bigramView_width;

	var <pulseWidth = 100;
	var <bigramHeight = 120; // one octave
	var <numOctaves;

	// 	var <bigramSpace = 30; //vertical
	// 	var <bigramSubdivisionSpace; //= bigramSpace / 6;  //(2 bigramSpace in a octave)
	// 	// margins of editView
	// 	var <upperSpace = 50;
	// 	var <lowerSpace = 50;
	// 	var <leftSpace = 50;
	// 	var <rightSpace = 50;
	//
	// 	var <beatLineSpace = 40; //horizontal (time subdivision)
	// 	var <beatSubdivision = 4;
	//
	// 	var <numBeats;
	// 	var <numOctaves = 8;
	//
	var lastMouseDownPosition;
	var selectionRectangle;
	var noteSelectedInLastDown;
	//
	// var <circleRadius = 20;
	//
	// 	var <mode;
	//
	//
	// internal representation
	//
	// references to the context
	var <editor;
	var <track;
	var <region;
	var <regionName;

	// parameters
	var <startBPD, <startPulseIndex;
	var <endBPD, <endPulseIndex;
	var <numPulses;
	var <>mode;

	var <divisionsFromPulses;
	var <barsFromPulses;
	var <pulsesFromPulseIndex;

	var <bpdList;

	var <holdSelection = false;

	//gui
	// var <showBigramSubdivision;
	//
	// 	// edit view limits
	// 	var <maxRightPosition, <maxDownPosition;
	//
	//
	*new { |editorWindow,track,region,regionName|
		^super.new("Bigram Region Editor",scroll:true).init(editorWindow,track,region,regionName);
	}

	init  { |myEditorWindow, myTrack, myRegion, myRegionName|

		// configure
		this.front;
		this.view.hasBorder_(false);
		this.view.autohidesScrollers_(true);
		//get reference to the region and the bigram editor window
		editor = myEditorWindow;
		track = myTrack;
		region = myRegion;
		regionName = myRegionName;

		numOctaves = editor.bigramEditor.numOctaves;

		startBPD = region.startBPD;
		endBPD = region.endBPD;

		/*startPulseIndex = editor.bigramEditor.getPulseIndexFromBPD(startBPD);
		endPulseIndex = editor.bigramEditor.getPulseIndexFromBPD(endBPD);*/

		numPulses = region.numPulses;

		showBigramSubdivision = false;

		// get division structure

		/*barsFromPulses = editor.bigramEditor.barsFromPulses[startPulseIndex..endPulseIndex];*/


		/*		divisionsFromPulses = Array.new(numPulses);
		numPulses.do { |pulse|
		var bar = barsFromPulses.at(pulse);
		var divisionsAtBar = editor.bigramEditor.measuresFromBars.at(bar).division;
		divisionsFromPulses.add(divisionsAtBar);
		};*/

		/*		pulsesFromPulseIndex = List.new;
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
		}.value();*/

		/*		bpdList = List.new;
		divisionsFromPulses.do{ |divisions,pulseIndex|
		divisions.do { |d|
		var bar = barsFromPulses.at(pulseIndex);
		bpdList.add(BPD.new(bar,pulsesFromPulseIndex[pulseIndex],d))
		}
		};*/

		/*		bpdList.do(_.print);*/



		//
		// 	init { |myNumBeats|
		// 		var width = this.bounds.width;
		// 		var height = this.bounds.height;
		//
		// 		numBeats = myNumBeats ? 16;
		//
		showBigramSubdivision = false;
		//
		// 		bigramSubdivisionSpace = bigramSpace / 6;  //(2 bigramSpace in a octave)
		//
		mode = \insert;
		//
		// 		bigram = Bigram.new;
		//
		// 		GUI.qt; // osx compatibility
		//
		//
		// 		// instanciate buttons
		//
		insertButton = Button(this, Rect(0, 0, elementSize, elementSize));
		editButton = Button(this, Rect(elementSize, 0, elementSize, elementSize));

		buttonList=List.new;
		buttonList.add(insertButton);
		buttonList.add(editButton);

		insertButton.states = [["I", Color.grey, Color.white], ["I", Color.white, Color.grey]];
		editButton.states = [["E", Color.grey, Color.white], ["E", Color.white, Color.grey]];

		insertButton.action_({|b| buttonList.do{|e|e.value=0}; b.value=1; mode=\insert; region.deselectAllNotes; this.refresh });
		editButton.action_({| b|buttonList.do{|e|e.value=0}; b.value=1; mode=\edit; this.refresh });

		insertButton.value_(1); //TODO: this is not working!!
		editButton.value_(0);


		//// play button -> outside of buttonList (not a mode button)
		playButton = Button(this, Rect(3*elementSize, 0, elementSize, elementSize));
		playButton.states = [["P", Color.blue, Color.grey(0.9)]/*,["P", Color.white, Color.blue(0.75)]*/];

		playButton.action_({ |b|
			var pbind = region.createPatterns;
			pbind.play;
/*			if (region.eventStreamPlayer.isPlaying.not) {
				region.play;
			};*/
		});

		// showBigramSubdivisionButton

		showBigramSubdivisionButton = Button (this, Rect(5*elementSize, 0, elementSize, elementSize));
		showBigramSubdivisionButton.states = [["s", Color.grey, Color.white], ["s", Color.white, Color.grey]];
		showBigramSubdivisionButton.action_({ |b|
			if (b.value == 0) {
				showBigramSubdivision = false;
			} {
				showBigramSubdivision = true;
			};
			this.refresh;
		});


		zoomSlider = Slider2D.new(this, Rect(8*elementSize,0,elementSize*4,elementSize)).x_(0.5).y_(0.5);
		zoomSlider.action_({ |slider|

			pulseWidth = slider.x.linlin(0,1,10,90);
			bigramHeight = slider.y.linlin(0,1,200,50);
			//recalculate bigramView size
			this.resizeCanvas;

		});


/*		zoomPlusHButton = Button(this,Rect(8*elementSize,0,elementSize,elementSize));
		zoomPlusHButton.states_([["+",Color.black,Color.grey(0.9)]]);
		zoomPlusHButton.action_({
			// change pulse width
			pulseWidth = pulseWidth + 10;
			// resize canvas
			this.resizeCanvas;
		});

		zoomMinusHButton = Button(this,Rect(9*elementSize,0,elementSize,elementSize));
		zoomMinusHButton.states_([["-",Color.black,Color.grey(0.9)]]);
		zoomMinusHButton.action_({
			// change pulse width
			pulseWidth = pulseWidth - 10;
			// resize canvas
			this.resizeCanvas;
		});

		zoomPlusVButton = Button(this,Rect(11*elementSize,0,elementSize,elementSize));
		zoomPlusVButton.states_([["+",Color.black,Color.grey(0.9)]]);
		zoomPlusVButton.action_({
			// change pulse width
			bigramHeight = bigramHeight + 10;
			// resize canvas
			this.resizeCanvas;
		});

		zoomMinusVButton = Button(this,Rect(12*elementSize,0,elementSize,elementSize));
		zoomMinusVButton.states_([["-",Color.black,Color.grey(0.9)]]);
		zoomMinusVButton.action_({
			// change pulse width
			bigramHeight = bigramHeight - 10;
			// resize canvas
			this.resizeCanvas;
		});*/

		//
		//
		// 		// SAVE AND LOAD FILES
		//
		// 		saveButton = Button(this, Rect(9*elementSize, 0, 2*elementSize, elementSize));
		// 		saveButton.states = [["save", Color.grey, Color.white]];
		// 		saveButton.action_({
		// 			Dialog.savePanel({ |path|
		// 				this.bigram.saveBigram(path);
		// 				("File " ++ path ++ " saved").postln;
		//
		// 			});
		// 		});
		//
		// 		loadButton = Button(this, Rect(11*elementSize, 0, 2*elementSize, elementSize));
		// 		loadButton.states = [["load", Color.grey, Color.white]];
		// 		loadButton.action_({
		// 			Dialog.openPanel({ |path|
		// 				this.bigram.loadBigram(path);
		// 				("File " ++ path ++ " opened").postln;
		// 				this.refresh;
		// 			});
		//
		// 		});
		//
		// 		// instanciate subdivision menu
		//
		// 		beatSubdivisionMenu = PopUpMenu(this,Rect(6*elementSize,0,2*elementSize,elementSize));
		// 		beatSubdivisionMenu.items = ["1","2","3","4","5","6","7","8"];
		// 		beatSubdivisionMenu.value = beatSubdivision - 1; //starts in 0
		// 		beatSubdivisionMenu.action = { |menu|
		// 			beatSubdivision = menu.items[menu.value].asInt;
		// 			this.refresh;
		// 		};
		//


		// bigram view limits
		bigramView_height = bigramHeight * numOctaves;
		bigramView_width = pulseWidth * (numPulses );

		/*		maxRightPosition = leftSpace + (numBeats * beatLineSpace);
		maxDownPosition = upperSpace + ((2*numOctaves) * bigramSpace);*/
		//
		// instanciate view
		bigramView = UserView(this,Rect(margin, elementSize.max(margin), bigramView_width, bigramView_height)).background_(Color.white);

		bigramView.drawFunc = {
			this.drawBigram;
			this.drawNotes;
			this.drawSelectionRectangle;
			editor.updateView; // update note changes in the bigram editor window
		};
		//
		//
		//
		//
		// 		// define actions
		//
		bigramView.mouseDownAction = this.mouseDownAction;
		bigramView.mouseMoveAction = this.mouseMoveAction;
		bigramView.mouseUpAction = this.mouseUpAction;
		//
		bigramView.keyDownAction = this.keyDownAction;
		bigramView.keyUpAction = this.keyUpAction;
		//
		// 	}
		//
		// 	setBeatSubdivision { |newBeatSubdivision|
		// 		beatSubdivision = newBeatSubdivision;
		// 		this.refresh;
		// 	}
		//
		//

	}

	resizeCanvas {
		bigramView_height = bigramHeight * numOctaves;
		bigramView_width = pulseWidth * (numPulses );
		bigramView.bounds_(Rect(margin, elementSize.max(margin), bigramView_width, bigramView_height));
		this.refresh;

	}

	drawBigram {
		var width = this.bounds.width;
		var height = this.bounds.height;

		var numBeatSubdivisionsToDraw, beatSubdivisionLineSpace;

		/////////// HORIZONTAL LINES ////////////
		//
		// bigram lines
		//

		Pen.strokeColor = Color.black;
		(2*numOctaves + 1).do{ |i| // two octaves
			if (i.even) {
				Pen.width = 1.5;
			} {
				Pen.width = 0.75;
			};

			Pen.line(0@(i*bigramHeight/2), bigramView_width@(i*bigramHeight/2));
			// Pen.line(leftSpace@(i*bigramSpace+upperSpace),maxRightPosition@(i*bigramSpace+upperSpace));
			Pen.stroke;
		};

		//
		// bigram subdivisions
		//

		if (showBigramSubdivision) {

			var subdivisionHeight = bigramHeight / 6;

			Pen.strokeColor = Color.red;
			Pen.width = 0.2;
			(12 * numOctaves).do {|i| //12 subdivisions per octave

				Pen.line(0@(i*subdivisionHeight), bigramView_width@(i*subdivisionHeight));
				/*Pen.line(leftSpace@(i*bigramSubdivisionSpace+upperSpace),maxRightPosition@(i*bigramSubdivisionSpace+upperSpace))*/
			};
			Pen.stroke;
		};
		//
		// //
		// //
		// //
		///////// VERTICAL LINES ////////////

		// pulse lines

		Pen.strokeColor = Color.black;
		Pen.width = 0.4;
		(numPulses+1).do{ |i|
			Pen.line((i*pulseWidth)@0,(i*pulseWidth)@bigramView_height);
			// Pen.line((i*beatLineSpace+leftSpace)@upperSpace,(i*beatLineSpace+leftSpace)@maxDownPosition);
		};
		Pen.stroke;


		// pulse subdivision lines

		Pen.width = 0.2;
		region.divisionsFromPulses.do{ |divisions,pulse|
			divisions.do { |d|
				Pen.line(
					Point((pulse*pulseWidth)+(d*pulseWidth/divisions),0),
					Point((pulse*pulseWidth)+(d*pulseWidth/divisions),bigramView_height)
				);
			}
		};
		Pen.stroke;

		// bar lines

		Pen.width = 1;
		region.barsFromPulses.do { |bar,pulse|
			if (bar != region.barsFromPulses[(pulse-1).wrap(0,region.barsFromPulses.size-1)]) {
				Pen.line(
					Point(pulse*pulseWidth,0),
					Point(pulse*pulseWidth,bigramView_height)
				);
			}
		};
		Pen.stroke;


	}

	// get a bigramCoordinate [bpd,absPitch]
	getBigramNote { |x,y|
		var pulseDivisionWidth = pulseWidth / 2;
		var bigramDivisionHeight = bigramHeight / 12;

		var divisionIndex = (x / pulseDivisionWidth).round;
		var yPos = ((numOctaves * 12) - (y / bigramDivisionHeight)).round;

		// turn xPos into bpd
		var bpd = region.bpdList.at(divisionIndex.clip(0,region.bpdList.size-1));
		var pitch = BPitch.new((yPos/12).floor,yPos%12);
		// "+++".postln;

		^BigramNote.new(pitch,bpd);
	}


	mouseDownAction {^{ |view, x, y, modifiers, buttonNumber, clickCount|
		var note, noteIndex;
		lastMouseDownPosition = Point(x,y);

		// [x,y] are the mouse down coordinates, in pixels
		// [xPos,yPos] are the bigram coordinates, in the form (absolutePos,totalPitch)
		note = this.getBigramNote(x,y);
		noteIndex = region.hasNote(note);

		/*		"mousePosition: ".post;
		[x,y].postln;
		"bigramPosition: ".post;
		[xPos,yPos].postln;*/

		switch (mode)
		{\insert} {
			region.deselectAllNotes;
			// if there is no note, place one
			if (noteIndex.isNil) {
				note.isSelected_(true);
				region.putNote(note);
			} {
				// if there is note, remove it
				region.removeNoteAt(noteIndex);
			}
		}
		{\edit} {
			// region.deselectAllNotes;
			if (noteIndex.isNil.not) {
				noteSelectedInLastDown = noteIndex;
				// if there is note, [de]select it
				// region.getNoteAt(noteIndex).isSelected_(region.getNoteAt(noteIndex).isSelected.not);
			} {
				// if not, start selection area
				noteSelectedInLastDown = nil;
			}
		};

		/*		"```".postln;
		bpdList.do(_.print);*/
		// region.notes;

		/*switch (mode)

		/////////////////////////// INSERT MODE /////////////////////
		{\insert} {
		//create new BigramNote
		var newNote, noteIndex;
		var pitchClass, height;
		var beat, relativePos;

		//calculate vertical position
		pitchClass = yPos % 12;
		height = (yPos / 12).floor;

		//calculate horizontal position

		beat = xPos.trunc;
		relativePos = xPos - beat;

		newNote = BigramNote.new(height,pitchClass,beat,relativePos);

		noteIndex = bigram.noteIndex(newNote);

		if (noteIndex.isNil) {
		//if there is no the same note in the same place, put the new note
		bigram.putNote(newNote)
		} {
		// if the is note, remove it
		bigram.removeNoteAt(noteIndex);
		};
		}

		/////////////////////////// EDIT MODE /////////////////////
		{\edit} {
		var newNote, noteIndex;
		var pitchClass, height;
		var beat, relativePos;

		//calculate vertical position
		pitchClass = yPos % 12;
		height = (yPos / 12).floor;

		//calculate horizontal position

		beat = xPos.trunc;
		relativePos = xPos - beat;

		// if there is a note in that position, select/deselect it
		newNote = BigramNote(height,pitchClass,beat,relativePos);
		noteIndex = bigram.noteIndex(newNote);
		if (noteIndex.isNil.not) {

		noteSelectedInLastDown = true;

		newNote = bigram.getNoteAt(noteIndex); // newNote now points to the existent note instance

		if (newNote.isSelected.not) {
		//if not selected, select
		newNote.isSelected_(true)
		}{
		//if selectec, deselect
		newNote.isSelected_(false)
		}
		} {

		// if there is no note in that position, create a selection rectangle!
		noteSelectedInLastDown = false;
		}
		}
		;*/
		this.refresh;
	}}

	mouseMoveAction { ^{ |view, x, y, modifiers|

		// region.deselectAllNotes;

		if (mode == \edit) {
			if (noteSelectedInLastDown.isNil.not) {
				// noteIndex
				var newNote = this.getBigramNote(x,y);
				var lastSelectedNote = region.getNoteAt(noteSelectedInLastDown);
				var diffPitch = newNote.pitch.absPitch - lastSelectedNote.pitch.absPitch;
				var diffDiv;
				var newNoteIndex, lastNoteIndex;
				region.bpdList.do{ |bpd,i|
					if (bpd.equal(newNote.bpd)) {newNoteIndex = i};
					if (bpd.equal(lastSelectedNote.bpd)) {lastNoteIndex = i};
				};

				diffDiv = newNoteIndex - lastNoteIndex;
				// newNote.print;
				/*				lastSelectedNote.print;*/
				// [diffPitch,diffDiv].postln;


				if (diffPitch > 0) {
					region.moveSelectedNotes(\up,diffPitch)
				} {
					region.moveSelectedNotes(\down,diffPitch.abs)
				};

				if (diffDiv > 0) {
					region.moveSelectedNotes(\right,diffDiv.abs)
				} {
					region.moveSelectedNotes(\left,diffDiv.abs)
				};


				/*region.getSelectedNotes.do{ |n|

				n.pitch_(newNote.pitch);
				n.bpd_(newNote.bpd)
				};*/
				this.refresh;

			} {
				//draw rectangle
				var note = this.getBigramNote(x,y);
				var lastNote = this.getBigramNote(lastMouseDownPosition.x,lastMouseDownPosition.y);

				if (holdSelection.not) {
					region.deselectAllNotes;
				};

				// [note.print,lastNote.print];
				/*			"****\n*****************".postln;
				"BETWEEN".postln;*/
				/*			region.getNotesBetweenBpd(note.bpd,lastNote.bpd).do{|n|
				n.isSelected_(true);
				};

				region.getNotesBetweenPitch(note.pitch,lastNote.pitch).do{|n|
				n.print;
				"!ª!!·$!·$!$·!".postln;
				};*/

				region.getNotesBetween(note,lastNote).do{|n|
					n.isSelected_(true);
				};

				// update selection rectangle
				selectionRectangle = Rect.fromPoints(lastMouseDownPosition,x@y);
				this.refresh;
			}

		}

		// if (mode == \edit and:{noteSelectedInLastDown.not}) {
		// get current bigram coordinate
		/*			var note = this.getBigramNote(x,y);
		var lastNote = this.getBigramNote(lastMouseDownPosition.x,lastMouseDownPosition.y);

		// [note.print,lastNote.print];
		/*			"****\n*****************".postln;
		"BETWEEN".postln;*/
		/*			region.getNotesBetweenBpd(note.bpd,lastNote.bpd).do{|n|
		n.isSelected_(true);
		};

		region.getNotesBetweenPitch(note.pitch,lastNote.pitch).do{|n|
		n.print;
		"!ª!!·$!·$!$·!".postln;
		};*/

		region.getNotesBetween(note,lastNote).do{|n|
		n.isSelected_(true);
		};

		// update selection rectangle
		selectionRectangle = Rect.fromPoints(lastMouseDownPosition,x@y);
		this.refresh;*/

		// };

	}}

	//
	// 	mouseMoveAction { ^{ |view, x, y, modifiers|
	// 		var selectedNotes, deselectedNotes;
	// 		var xPos, yPos;
	//
	// 		var beat1, relativePos1, beat2, relativePos2;
	// 		var height1, pitchClass1, height2, pitchClass2;
	//
	//
	// 		if (mode == \edit and:{noteSelectedInLastDown.not}) {
	//
	// 			#xPos,yPos = this.getBigramCoordinate(x,y);
	// 			selectionRectangle = Rect.fromPoints(lastMouseDownPosition,x@y);
	//
	// 			// convert to required separate format
	// 			#beat1,relativePos1 = BigramNote.getRelativePos(this.getMouseXPosition(lastMouseDownPosition.x));
	// 			#beat2,relativePos2 = BigramNote.getRelativePos(xPos);
	// 			#height1,pitchClass1 = BigramNote.getRelativePitch(this.getMouseYPosition(lastMouseDownPosition.y));
	// 			#height2,pitchClass2 = BigramNote.getRelativePitch(yPos);
	//
	//
	// 			// select notes inside the rectangle
	// 			selectedNotes = bigram.getNotesInRange(beat1, relativePos1, beat2, relativePos2, height1, pitchClass1, height2, pitchClass2);
	// 			bigram.selectNotes(selectedNotes);
	//
	// 			// deselect notes which are now not selected
	// 			deselectedNotes = List.new; // it could be an array, we know sizes of notes and selectedNotes
	// 			bigram.notes.do{ |note|
	// 				if (selectedNotes.indexOf(note).isNil) {
	// 					deselectedNotes.add(note)
	// 				}
	// 			};
	// 			bigram.deselectNotes(deselectedNotes);
	//
	// 			this.refresh;
	// 		};
	// 	}}
	//
	mouseUpAction { ^{ |view, x, y, modifiers|
		if (mode == \edit) {
			selectionRectangle = nil;

			// move notes
			/*			if (noteSelectedInLastDown.isNil.not) {
			if (~diffPitch > 0) {
			region.moveSelectedNotes(\up,~diffPitch)
			} {
			region.moveSelectedNotes(\down,~diffPitch.abs)
			};

			if (~diffDiv > 0) {
			region.moveSelectedNotes(\right,~diffDiv.abs)
			} {
			region.moveSelectedNotes(\left,~diffDiv.abs)
			};
			~diffPitch = 0;
			~diffDiv = 0;
			};*/
		};
		this.refresh;

	}}


	// 	mouseUpAction { ^{ |view, x, y, modifiers|
	// 		if (mode == \edit) {
	// 			selectionRectangle = nil;
	// 		};
	// 		this.refresh;
	// 	}}
	//
	keyDownAction { ^{ |view, char, modifiers, unicode, keycode, key|
		key.postln;
		//
		//global
		switch (key)
		{73} {insertButton.valueAction_(1)} // I: insert mode
		{69} {editButton.valueAction_(1)} //E: edit mode
		;

		//edit
		// if (mode==\edit) {
		switch (key)

		// up arrow: move up 1 semitone selected notes
		{16777235} {region.moveSelectedNotes(\up,1)}

		// down arrow: move down 1 semitone selected notes
		{16777237} {region.moveSelectedNotes(\down,1)}

		// left arrow: move left 1 subdivision selected notes
		{16777234} {region.moveSelectedNotes(\left,1)}

		// right arrow: move right 1 subdivision selected notes
		{16777236} {region.moveSelectedNotes(\right,1)}

		// supr: remove selected notes
		{16777223} {region.removeSelectedNotes}

		// control: hold selection
		{16777249} {holdSelection=true}
		;
		// };
		this.refresh;
	}}

	keyUpAction { ^{ |view, char, modifiers, unicode, keycode, key|
		// if (mode==\edit) {
		switch (key)
		// control: hold selection
		{16777249} {holdSelection=false}
		;
		// }
	}}

	//
	// 	getBigramCoordinate { |x,y|
	// 		^[this.getMouseXPosition(x),this.getMouseYPosition(y)]
	// 	}
	//
	// 	getMouseXPosition { |x|		//calculates time subdivision from mouse position and offsets
	//
	// 		var pos;
	// 		if (x >= leftSpace) {
	// 			if (x <= maxRightPosition) {
	//
	// 				// inside lattice
	//
	// 				pos = x - leftSpace; // quit offset
	// 				pos = pos / beatLineSpace;// <--------- TODO: adjust to be parametrizable!!
	// 				//adjust to current beat subdivision
	// 				pos = pos.round(1/beatSubdivision);
	// 			} {
	//
	// 				// right from lattice
	//
	// 				pos = maxRightPosition;
	// 			}
	// 		} {
	//
	// 			// left from lattice
	//
	// 			pos = 0;
	// 		};
	//
	//
	// 		^pos;
	// 	}
	//
	// 	getMouseYPosition { |y|
	// 		//calculates pitch subdivision from mouse position and offsets
	// 		//numeration starts at 0 = minimum pitch
	// 		var pos;
	// 		if (y >= upperSpace) {
	// 			if (y <= maxDownPosition) {
	//
	// 				// inside lattice
	//
	// 				pos = y - upperSpace;
	// 				pos = (pos / bigramSubdivisionSpace).round;
	// 			} {
	//
	// 				// below lattice
	//
	// 				pos = maxDownPosition;
	// 			}
	//
	// 		} {
	//
	// 			// above lattice
	//
	// 			pos = 0;
	// 		};
	//
	// 		// turn up-down for convenience
	// 		pos = (numOctaves * 12) - pos;
	//
	//
	// 		^pos;
	// 	}
	//

	getBigramCoordinate { |note|

		var subdivisionIndex = region.getSubdivisionIndex(note);
		// var subdivisionIndex = region.bpdList.indexOf(note.bpd);
		var absPitch = (numOctaves * 12) - note.pitch.absPitch;

		var x = subdivisionIndex * pulseWidth / 2;
		var y = absPitch * bigramHeight / 12;

		^[x,y]

		////////
		/*		var pulseDivisionWidth = pulseWidth / 2;
		var bigramDivisionHeight = bigramHeight / 12;

		var divisionIndex = (x / pulseDivisionWidth).round;
		var yPos = ((numOctaves * 12) - (y / bigramDivisionHeight)).round;

		// turn xPos into bpd
		var bpd = bpdList.at(divisionIndex).print;
		var pitch = BPitch.new((yPos/12).floor,yPos%12).print;
		"+++".postln;

		^BigramNote.new(pitch,bpd);*/
	}


	drawNotes {
		var circleRadius = min(pulseWidth/2,bigramHeight/12);
		"DRAW NOTES".postln;

		region.getAllNotes.do{ |note|
			var x,y;
			#x,y = this.getBigramCoordinate(note);


			if (note.getType == \w) {
				Pen.fillColor = Color.white;
			} {
				Pen.fillColor = Color.black;
			};

			// if (mode == \edit) {
			if (note.isSelected) {
				Pen.strokeColor = Color.blue;
				Pen.width = 2;
			} {
				Pen.strokeColor = Color.black;
				Pen.width = 1;
			};
			// } {
			// 	Pen.strokeColor = Color.black;
			// 	Pen.width = 1;
			// };

			Pen.addArc(x@y,circleRadius,0,2pi);
			Pen.fillStroke;
			// note.print;




		}
	}
	//
	// 			var beat = note.beat;
	// 			var relativePos = note.relativePos;
	// 			var height = note.height;
	// 			var pitchClass = note.pitchClass;
	//
	// 			var xPos, yPos;
	// 			var subdivisionTotal = (beat + relativePos) * beatSubdivision;
	// 			var pitchTotal = (height*12) + pitchClass;
	// 			//invert pitchTotal for the view
	// 			pitchTotal = (numOctaves * 12) - pitchTotal;
	//
	//
	// 			xPos = leftSpace + (subdivisionTotal*beatLineSpace/beatSubdivision);
	// 			yPos = upperSpace + (pitchTotal*bigramSubdivisionSpace);
	//
	// 			//draw notes
	// 			if (note.type == \w) {
	// 				Pen.fillColor = Color.white;
	// 			} {
	// 				Pen.fillColor = Color.black;
	// 			};
	//
	// 			if (mode == \edit){
	// 				if (note.isSelected) {
	// 					Pen.strokeColor = Color.blue;
	// 					Pen.width = 2;
	// 				} {
	// 					Pen.strokeColor = Color.black;
	// 					Pen.width = 1;
	// 				};
	// 			} {
	// 				Pen.strokeColor = Color.black;
	// 				Pen.width = 1;
	// 			};
	//
	// 			Pen.addArc(xPos@yPos,circleRadius,0,2pi);
	// 			Pen.fillStroke;
	// 		}
	// 	}
	//

	drawSelectionRectangle {
		Pen.color = Color.blue;
		Pen.alpha = 0.2;
		Pen.addRect(selectionRectangle);
		Pen.fill;
	}
	//
	//
	//
}
//
//
