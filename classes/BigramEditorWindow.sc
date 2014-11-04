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

BigramEditorWindow {

	var window;
	var bounds;

	// file options and bars, bpm visualization
	var <bigramOptionsView;
	var <bigramOptionsView_height;

	var <bigramOptionsMenuView;
	var <bigramOptionsMenuView_height;
	var <bigramOptionsMenuElements;
	var <bigramOptionsMenuElements_width;

	var <bigramOptionsModeView;
	var <modeButtons; // List
	var <pointerModeButton,<cutModeButton,<penModeButton, <zoomModeButton,<glueModeButton,<rubberModeButton;
	var <zoomSlider;

	var <barTrack;
	var <tempoTrack;
	var <measureTrack;

	// tracks visualization
	var <bigramView;
	var <bigramView_height;

	var <bigramCanvasView;
	var <bigramCanvasView_width;
	var <bigramCanvas;
	var <bigramCanvas_width;
	var <bigramCanvas_height;

	var <bigramTracksOptionsView;
	var <bigramTracksOptionsView_width;


	var <bigramControlBarsView;
	var <bigramControlBarsView_height;


	var <pulseWidth = 40;
	var <trackHeight = 100;

	var <scrollSliderSize = 17; // empirical!

	var <>mode; // mouse interaction mode

	/////////

	var <bigramEditor;
	var <trackControls;
	var <trackControlNames;

	var <startRegionBPD;
	var <endRegionBPD;

	var <>selectedTrackIndex;

	var <>savePath;
	var <>holdSelection = false;
	var <>cursorPosition; // for drawing
	var <>cursorBPD;
	var <>cursorPulseIndex;

	var <>resizeRegion;
	var <>resizeDir;
	var <>resizeTrack;
	var <>moveRegion;
	var <>moveTrack;
	var <>moveIndex;

	var <>trackControls_name;
	var <>trackControls_recButton;
	var <>trackControls_soloButton;
	var <>trackControls_muteButton;
	var <>trackControls_midiOut;
	var <>trackControls_pan;
	var <>trackControls_volume;

	var <>backButton;
	var <>playButton;
	var <>loopButton;
	var <>recButton;

	var <>loop = false;
	var <>loopPulseIndex;

	var <>midiOut;
	var <>midiInstruments;


	///////////////////////// INITIALIZATION /////////////////////////

	*new {
		^super.new.init;
	}

	init {

		bigramEditor = BigramEditor.new(this);
		cursorPulseIndex = 0;

		trackControls = List.new;
		trackControlNames = List.new;

		//////////// midi //////////////////////

		this.configureMidi;

		///////////////////// gui /////////////////////

		GUI.qt; // osx compatibility

		bounds = Window.availableBounds;
		// create window
		window= Window.new("Bigram Editor",bounds:bounds,resizable:true,scroll:false).front;

		window.view.onResize = { |view|
			this.recalculateScrollViewSizes(view);
		};

		///////// close actions ///////
		window.view.onClose = {
			this.onCloseActions;
		};




		////// static upper elements

		//create composite view for static elements
		bigramOptionsView_height = bounds.height/6;
		bigramOptionsView = CompositeView(window,Rect(0, 0, bounds.width, bigramOptionsView_height));
		bigramOptionsView.background_(Color.black);

		// create bigram options menu
		bigramOptionsMenuView_height = bigramOptionsView_height / 2;
		bigramOptionsMenuView = CompositeView(bigramOptionsView,Rect(0,0,bounds.width,bigramOptionsMenuView_height));
		bigramOptionsMenuView.background_(Color.grey(0.2));

		bigramOptionsMenuElements =
		[
			["File", "new", "open", "close", "save", "saveAs", "import", "export", "print", "configure"],
			/*			["Edit", "undo", "redo", "cut", "copy", "paste", "delete", "group", "ungroup", "block", "unblock", "preferencies"],*/
			["Region", "duplicate", "group", "split", "block", "unblock"],
			["Track", "new","duplicate","delete"],
			["Window"]
		];
		bigramOptionsMenuElements_width = 80;

		bigramOptionsMenuView.mouseDownAction = { |view, x, y, modifiers, buttonNumber|
			var row = (y / bigramOptionsMenuView_height * 4).floor.asInt;
			var column = ((x-10) / bigramOptionsMenuElements_width).abs.floor.asInt;
			[row,column].postln;
			this.menuElementAction(row,column);
			{
				// highlight option
				var absIndex = 0;
				(row).do { |r| absIndex = absIndex + bigramOptionsMenuElements[r].size };
				absIndex = absIndex + column;
				view.children[absIndex].stringColor_(Color.red);
			}.();

		};

		bigramOptionsMenuView.mouseUpAction = { |view|
			//de-highlight all options
			view.children.do(_.stringColor_(Color.white));
		};

		//place menu items
		bigramOptionsMenuView.addFlowLayout(10@0,0@0);
		/*		bigramOptionsMenuElements.do { |e|			StaticText(bigramOptionsMenuView,Rect(0,0,bigramOptionsMenuElements_width,bigramOptionsMenuView_height)).string_(e).stringColor_(Color.white);
		};*/
		bigramOptionsMenuElements.do { |element|
			element.do { |e,i|	var text = StaticText(bigramOptionsMenuView,Rect(0,0,bigramOptionsMenuElements_width,bigramOptionsMenuView_height/4));
				text.string_(e);
				text.stringColor_(Color.white)
				/*				if (i ==0) {
				text.stringColor_(Color.grey(0.75))
				} {
				text.stringColor_(Color.white)
				};*/
			};
			bigramOptionsMenuView.decorator.nextLine;
		};

		// mode buttons
		// TODO: move to other place!!!<
		bigramOptionsModeView = CompositeView(bigramOptionsView,Rect(0,bigramOptionsMenuView_height, bounds.width,bigramOptionsMenuView_height));
		bigramOptionsModeView.background_(Color.grey(0.6));
		bigramOptionsModeView.addFlowLayout(10@0,0@0);

		modeButtons = List.new;

		pointerModeButton = Button.new(bigramOptionsModeView,Rect(0,0,50,bigramOptionsMenuView_height));
		pointerModeButton.states_([["pointer",Color.black,Color.grey(0,2)],["pointer",Color.black,Color.grey(0.8)]]).action_({|b|modeButtons.do(_.value_(0));b.value_(1);mode=\pointer}).value_(1);
		modeButtons.add(pointerModeButton);

		penModeButton = Button.new(bigramOptionsModeView,Rect(0,0,50,bigramOptionsMenuView_height));
		penModeButton.states_([["pen",Color.black,Color.grey(0,2)],["pen",Color.black,Color.grey(0.8)]]).action_({|b|modeButtons.do(_.value_(0));b.value_(1);mode=\pen}).value_(0);
		modeButtons.add(penModeButton);


		rubberModeButton = Button.new(bigramOptionsModeView,Rect(0,0,50,bigramOptionsMenuView_height));
		rubberModeButton.states_([["rubber",Color.black,Color.grey(0,2)],["rubber",Color.black,Color.grey(0.8)]]).action_({|b|modeButtons.do(_.value_(0));b.value_(1);mode=\rubber}).value_(0);
		modeButtons.add(rubberModeButton);

		/*		zoomModeButton = Button.new(bigramOptionsModeView,Rect(0,0,50,bigramOptionsMenuView_height));
		zoomModeButton.states_([["zoom",Color.black,Color.grey(0,2)],["zoom",Color.black,Color.grey(0.8)]]).action_({|b|modeButtons.do(_.value_(0));b.value_(1);mode=\zoom}).value_(0);
		modeButtons.add(zoomModeButton);*/

		/*		cutModeButton = Button.new(bigramOptionsModeView,Rect(0,0,50,bigramOptionsMenuView_height));
		cutModeButton.states_([["cut",Color.black,Color.grey(0,2)],["cut",Color.black,Color.grey(0.8)]]).action_({|b|modeButtons.do(_.value_(0));b.value_(1);mode=\cut}).value_(0);
		modeButtons.add(cutModeButton);*/

		/*		glueModeButton = Button.new(bigramOptionsModeView,Rect(0,0,50,bigramOptionsMenuView_height));
		glueModeButton.states_([["glue",Color.black,Color.grey(0,2)],["glue",Color.black,Color.grey(0.8)]]).action_({|b|modeButtons.do(_.value_(0));b.value_(1);mode=\glue}).value_(0);
		modeButtons.add(glueModeButton);*/

		//set default mode
		mode = \pointer;

		// custom separator
		CompositeView(bigramOptionsModeView,Rect(0,0,100,10));

		// zoom slider
		zoomSlider = Slider2D.new(bigramOptionsModeView, Rect(0,0,200,bigramOptionsMenuView_height)).x_(0.5).y_(0.5);
		zoomSlider.action_({ |slider|

			pulseWidth = slider.x.linlin(0,1,10,90);
			trackHeight = slider.y.linlin(0,1,200,50);
			//recalculate bigramView size
			this.recalculateCanvasSize;

		});

		// play controls
		CompositeView(bigramOptionsModeView,Rect(0,0,100,10));

		backButton = Button(bigramOptionsModeView,Rect(0,0,50,bigramOptionsMenuView_height));
		backButton.states_([["back",Color.black,Color.grey(0,2)]]);
		backButton.action_({
			// position cursor at beggining
			cursorPulseIndex = 0;
			this.updateView;
		});

		playButton = Button(bigramOptionsModeView,Rect(0,0,50,bigramOptionsMenuView_height));
		playButton.states_([["play",Color.black,Color.grey(0,2)],["pause",Color.black,Color.grey(0.8)]]);
		playButton.action_({ |b|
			if (b.value == 1) {
				bigramEditor.play(cursorPulseIndex,loopPulseIndex,loop);
			} {
				bigramEditor.pause;
			}
		});

		loopButton =  Button(bigramOptionsModeView,Rect(0,0,50,bigramOptionsMenuView_height));
		loopButton.states_([["loop",Color.black,Color.grey(0,2)],["loop",Color.black,Color.grey(0.8)]]);
		loopButton.action_({
			loop = loop.not;
		});

		recButton = Button(bigramOptionsModeView,Rect(0,0,50,bigramOptionsMenuView_height));
		recButton.states_([["rec",Color.black,Color.grey(0,2)],["rec",Color.black,Color.grey(0.8)]]);



		/////////////////////////
		////// tracks

		// bar track
		bigramTracksOptionsView_width = bounds.width / 20;



		//create bigramView
		bigramView_height = bounds.height - bigramOptionsView_height;
		bigramView = ScrollView(window,Rect(0,bigramOptionsView_height,bounds.width,bigramView_height));
		bigramView.hasBorder_(false);
		bigramView.hasHorizontalScroller_(false);
		bigramView.autohidesScrollers_(false);
		bigramView.background_(Color.grey(0.4));

		bigramView.keyDownAction = this.bigramViewKeyDownAction;

		//bigramControlBarsView
		bigramControlBarsView_height = bigramView_height / 10;


		//create bigram tracks options

		bigramTracksOptionsView = CompositeView(bigramView,Rect(0, bigramControlBarsView_height, bigramTracksOptionsView_width,bigramView_height-bigramControlBarsView_height));
		bigramTracksOptionsView.background_(Color.grey(0.4));
		// bigramTracksOptionsView.addFlowLayout(0@0,0@0); //trackHeight

		////////////////////////
		////// canvas
		//
		// //create bigram canvas view
		bigramCanvasView_width = bounds.width - bigramTracksOptionsView_width;
		bigramCanvasView = ScrollView(bigramView,Rect(bigramTracksOptionsView_width,0, bigramCanvasView_width,bigramView_height));
		bigramCanvasView.hasBorder_(false);
		bigramCanvasView.background_(Color.grey(0.2));
		bigramCanvasView.hasVerticalScroller_(false);
		bigramCanvasView.autohidesScrollers_(false);

		// control bars
		bigramControlBarsView = CompositeView(bigramCanvasView,Rect(0,0,bigramControlBarsView_height,bigramCanvasView_width));
		bigramControlBarsView.background_(Color.grey(0.2));


		//create bigram user view (where the tracks are drawn)
		bigramCanvas_width = bigramEditor.numPulses * pulseWidth;
		bigramCanvas_height =  bigramEditor.bigramTracks.size;

		bigramCanvas = UserView(bigramCanvasView,Rect(0,bigramControlBarsView_height, bigramCanvas_width,bigramCanvas_height));
		bigramCanvas.background_(Color.white);
		bigramCanvas.drawFunc = { |view|
			this.drawCanvas(view);
		};
		//interaction
		bigramCanvas.mouseDownAction = this.bigramCanvasMouseDownAction;
		bigramCanvas.mouseMoveAction = this.bigramCanvasMouseMoveAction;
		bigramCanvas.mouseUpAction = this.bigramCanvasMouseUpAction;
		bigramCanvas.keyDownAction = this.bigramCanvasKeyDownAction;
		bigramCanvas.keyUpAction = this.bigramCanvasKeyUpAction;


		// options tracks
		barTrack = UserView(bigramControlBarsView,Rect(0,0, bigramCanvas_width,bigramControlBarsView_height/3));

		barTrack.background_(Color.grey(0.9));
		barTrack.drawFunc = { |view| this.drawBarTrack(view)};
		barTrack.mouseDownAction = { |view, x, y, modifiers, buttonNumber, clickCount|
			var pulseIndex = (x / pulseWidth).floor.asInteger;
			switch (buttonNumber)
			{0} { //left button: position pointer
				cursorPulseIndex = pulseIndex;
			}
			{1} { //rigth button: position loop
				loopPulseIndex = pulseIndex;
			};
			barTrack.refresh;
			bigramView.refresh;
		};

		tempoTrack = UserView(bigramControlBarsView,Rect(0,bigramControlBarsView_height/3, bigramCanvas_width,bigramControlBarsView_height/3 ));
		tempoTrack.background_(Color.grey(0.8));
		tempoTrack.drawFunc = { |view| this.drawTempoTrack(view)};

		measureTrack =  UserView(bigramControlBarsView,Rect(0,2*bigramControlBarsView_height/3, bigramCanvas_width,bigramControlBarsView_height/3 ));
		measureTrack.background_(Color.grey(0.9));
		measureTrack.drawFunc = { |view| this.drawMeasureTrack(view)};

		// track control holders
		trackControls_name = List.new;
		trackControls_recButton = List.new;
		trackControls_soloButton = List.new;
		trackControls_muteButton = List.new;
		trackControls_midiOut = List.new;
		trackControls_pan = List.new;
		trackControls_volume = List.new;

	}

	///////////////////////// DRAW FUNCTIONS /////////////////////////

	drawCanvas { |view|
		var lastBar;

		//draw pulse lines
		Pen.width = 0.8;
		Pen.strokeColor = Color.grey(0.5);
		bigramEditor.numPulses.do { |x|
			Pen.line((x*pulseWidth)@0,(x*pulseWidth)@bigramCanvas_height);
			Pen.stroke;
		};

		//draw bar lines
		Pen.width = 1.2;
		Pen.strokeColor = Color.black;
		bigramEditor.barsFromPulses.do { |bar,x|
			if (lastBar != bar) {
				Pen.line((x*pulseWidth)@0,(x*pulseWidth)@bigramCanvas_height);
				Pen.stroke;
				lastBar = bar;
			};
		};

		//draw track lines
		Pen.strokeColor = Color.black;
		bigramEditor.bigramTracks.do { |track,i|
			Pen.line(0@(trackHeight*(i+1)),bigramCanvas_width@(trackHeight*(i+1)));
			Pen.stroke;
		};


		//draw selected track
		if (selectedTrackIndex.isNil.not) {
			Pen.width = 1;
			Pen.strokeColor = Color.blue;
			Pen.fillColor = Color.blue(1,0.05);
			Pen.addRect(Rect(0,selectedTrackIndex*trackHeight,bigramCanvas_width,trackHeight));
			Pen.fillStroke;
		};

		//draw regions
		bigramEditor.bigramTracks.do { |track,trackNumber|
			track.bigramRegions.do { |region|
				// get track index from bpd
				var startPulseIndex = bigramEditor.getPulseIndexFromBPD(region.startBPD);
				var endPulseIndex = bigramEditor.getPulseIndexFromBPD(region.endBPD);

				Pen.addRect(Rect.newSides(startPulseIndex*pulseWidth,trackNumber*trackHeight, (endPulseIndex+1)*pulseWidth,(trackNumber+1)*trackHeight));

				if (region.selected) {
					Pen.width = 2;
					Pen.strokeColor = Color.green;
					Pen.fillColor = Color.green(0.9,0.2);
				} {
					Pen.width = 2;
					Pen.strokeColor = Color.green(0.5);
					Pen.fillColor = Color.green(0.6,0.1);
				};
				Pen.fillStroke;

				// draw resize cursor

				//left
				Pen.addRect(Rect.new(startPulseIndex*pulseWidth,(trackNumber*trackHeight)+(trackHeight*3/4), pulseWidth/2,trackHeight/4));
				if (region.selected) {
					Pen.width = 2;
					Pen.strokeColor = Color.green;
					Pen.fillColor = Color.green(0.9,0.2);
				} {
					Pen.width = 2;
					Pen.strokeColor = Color.green(0.5);
					Pen.fillColor = Color.green(0.6,0.1);
				};
				Pen.fillStroke;

				//right
				Pen.addRect(Rect.new((endPulseIndex*pulseWidth)+(pulseWidth/2),(trackNumber*trackHeight)+(trackHeight*3/4), pulseWidth/2,trackHeight/4));
				if (region.selected) {
					Pen.width = 2;
					Pen.strokeColor = Color.green;
					Pen.fillColor = Color.green(0.9,0.2);
				} {
					Pen.width = 2;
					Pen.strokeColor = Color.green(0.5);
					Pen.fillColor = Color.green(0.6,0.1);
				};
				Pen.fillStroke;



				// draw notes inside regions

				region.notes.do{ |note|
					/*					var a = region.bpdList.do(_.print);
					var b = note.bpd.print;
					var c = region.bpdList.indexOf(note.bpd).postln;
					var d = "asdfasdfasdfasdfasdf".postln;*/

					var subdivisionIndex = region.getSubdivisionIndex(note);
					// var subdivisionIndex = region.bpdList.indexOf(note.bpd).postln;
					var absPitch = (bigramEditor.numOctaves * 12) - note.pitch.absPitch;

					// region.getSubdivisionIndex(note).postln;
					/*					region.bpdList.do{ |bpd|
					bpd.print;
					note.bpd.print;
					bpd.equal(note.bpd).postln;
					"**".postln;
					}*/

					var x = (startPulseIndex*pulseWidth) + (subdivisionIndex * pulseWidth / 2);
					var y = (trackNumber*trackHeight) + (absPitch * trackHeight/ bigramEditor.numOctaves / 12);

					var circleRadius = min(pulseWidth/2,trackHeight/bigramEditor.numOctaves/12);
					Pen.width = 0.5;
					if (note.getType == \w)
					{Pen.fillColor = Color.white}
					{Pen.fillColor = Color.black};
					Pen.strokeColor = Color.black;
					Pen.addArc(x@y,circleRadius*2,0,2pi);
					Pen.fillStroke;
				}

			}
		};

		//draw position line
		if (cursorPulseIndex.isNil.not) {
			Pen.color = Color.red;
			Pen.line(
				Point(cursorPulseIndex*pulseWidth,0),
				Point(cursorPulseIndex*pulseWidth,bigramCanvas_height)
			);
			Pen.stroke;
		};

		// draw loop line
		if (loopPulseIndex.isNil.not) {
			Pen.width = 2;
			Pen.color = Color.blue;
			Pen.line(
				Point(loopPulseIndex*pulseWidth,0),
				Point(loopPulseIndex*pulseWidth,bigramCanvas_height)
			);
			Pen.stroke;
		};

	}

	drawBarTrack { |view|
		var lastBar;

		//draw pulse lines
		Pen.width = 0.8;
		Pen.strokeColor = Color.grey(0.5);
		bigramEditor.numPulses.do { |x|
			Pen.line((x*pulseWidth)@0,(x*pulseWidth)@barTrack.bounds.height);
			Pen.stroke;
		};

		//draw bar lines
		Pen.width = 1.2;
		Pen.strokeColor = Color.black;
		bigramEditor.barsFromPulses.do { |bar,x|
			if (lastBar != bar) {
				Pen.line((x*pulseWidth)@0,(x*pulseWidth)@barTrack.bounds.height);
				Pen.stroke;
				lastBar = bar;
			};
		};

		// draw cursor position
		if (cursorPulseIndex.isNil.not) {
			Pen.color = Color.red;
			Pen.line(
				Point(cursorPulseIndex*pulseWidth,0),
				Point(cursorPulseIndex*pulseWidth,barTrack.bounds.height)
			);
			Pen.stroke;
		};

		// draw loop position
		if (loopPulseIndex.isNil.not) {
			Pen.width = 2;
			Pen.color = Color.blue;
			Pen.line(
				Point(loopPulseIndex*pulseWidth,0),
				Point(loopPulseIndex*pulseWidth,barTrack.bounds.height)
			);
			Pen.stroke;
		};
	}

	drawTempoTrack { |view|
		var lastBar;
		//draw pulse lines and tempo numbers
		Pen.width = 0.8;
		Pen.strokeColor = Color.grey(0.5);
		bigramEditor.numPulses.do { |x,i|
			Pen.line((x*pulseWidth)@0,(x*pulseWidth)@barTrack.bounds.height);
			Pen.stroke;
			Pen.stringAtPoint(bigramEditor.tempos[i].asString,(x*pulseWidth@0))
		};
		//draw bar lines
		Pen.width = 1.2;
		Pen.strokeColor = Color.black;
		bigramEditor.barsFromPulses.do { |bar,x|
			if (lastBar != bar) {
				Pen.line((x*pulseWidth)@0,(x*pulseWidth)@barTrack.bounds.height);
				Pen.stroke;
				lastBar = bar;
			};
		};

	}

	drawMeasureTrack { |view|
		var lastBar;
		//draw bar lines and measures
		Pen.width = 1.2;
		Pen.strokeColor = Color.black;
		bigramEditor.barsFromPulses.do { |bar,x|
			if (lastBar != bar) {
				Pen.line((x*pulseWidth)@0,(x*pulseWidth)@barTrack.bounds.height);
				Pen.stroke;
				lastBar = bar;
				Pen.stringAtPoint(bigramEditor.measuresFromBars[bar].getDrawableString,(x*pulseWidth)@0)
			};
		};


	}

	updateView {
		this.recalculateScrollViewSizes;
		window.refresh;
	}

	setPulseWidth { |width|
		pulseWidth = width;

		// recalculate BigramCanvas width
		bigramCanvas_width = bigramEditor.numPulses * pulseWidth;

		bigramCanvas.bounds_(Rect(0,bigramControlBarsView_height, bigramCanvas_width,bigramCanvas_height));

		//recalculate controlBarViews bars
		barTrack.bounds_(Rect(0,0,bigramCanvas_width,bigramControlBarsView_height/3));

		bigramCanvas.refresh;
		this.updateView;

		// todo: también las marcas de compases en bigramOptionsView!!
	}

	// for all BigramView and BigramCanvasView scrollViews
	recalculateScrollViewSizes { |view|
		var width, height;
		var maxHeight;

		if (view.isNil) {view = window.view};
		width = view.bounds.width;
		height = view.bounds.height;

		bigramView_height = height - bigramOptionsView_height;
		bigramCanvasView_width = width - bigramTracksOptionsView_width - scrollSliderSize;

		bigramView.bounds_(Rect(0,bigramOptionsView_height,width,bigramView_height));

		if (bigramView_height > (bigramCanvas.bounds.height+bigramControlBarsView_height)) {
			maxHeight = bigramView_height;
		} {
			maxHeight = bigramCanvas.bounds.height+bigramControlBarsView_height;
			maxHeight = maxHeight + scrollSliderSize;
		};

		bigramCanvasView.bounds_(Rect(bigramTracksOptionsView_width,0, bigramCanvasView_width,maxHeight));

		bigramTracksOptionsView.bounds_(Rect(0,bigramControlBarsView_height,bigramTracksOptionsView_width,bigramCanvasView.innerBounds.height-bigramControlBarsView_height));

		bigramControlBarsView.bounds_(Rect(0,0,max(bigramCanvasView_width,bigramCanvas.bounds.width), bigramControlBarsView_height));

	}



	///////////////////////// TRACK MANAGING /////////////////////////

	addTrack {
		var trackName = bigramEditor.addTrack;

		// make the canvas bigger
		bigramCanvas_height = bigramCanvas_height + trackHeight;
		bigramCanvas.bounds_(Rect(0, bigramControlBarsView_height, bigramCanvas_width, bigramCanvas_height));

		// add TrackOptions controls and view
		this.addTrackControls(trackName);

		// refresh views
		this.updateView;

		^trackName;
	}

	duplicateTrack { |trackName|
		var newTrackName = bigramEditor.duplicateTrack(trackName);

		// make the canvas bigger
		bigramCanvas_height = bigramCanvas_height + trackHeight;
		bigramCanvas.bounds_(Rect(0, bigramControlBarsView_height, bigramCanvas_width, bigramCanvas_height));

		// add TrackOptions controls and view
		this.addTrackControls(newTrackName);

		// refresh views
		this.updateView;

	}

	removeTrack { |trackName|

		// remove track internally
		var ans = bigramEditor.removeTrack(trackName);

		if (ans.isNil.not) {

			// reduce canvas size
			bigramCanvas_height = bigramCanvas_height - trackHeight;
			bigramCanvas.bounds_(Rect(0, bigramControlBarsView_height, bigramCanvas_width, bigramCanvas_height));

			// delete track controls
			this.removeTrackControls(trackName);

		}
	}

	removeAllTracks {
		bigramEditor.removeAllTracks;

		// reduce canvas size
		bigramCanvas_height = 0;
		bigramCanvas.bounds_(Rect(0, bigramControlBarsView_height, bigramCanvas_width, bigramCanvas_height));

		// delete track controls
		this.removeAllTrackControls;


	}


	addTrackControls { |trackName|

		var name;
		var recButton, soloButton, muteButton;
		var midiOut;
		var pan;
		var volume;

		var track = bigramEditor.bigramTracks.at(bigramEditor.bigramTrackNames.indexOf(trackName));

		// create the composite view father of control widgets
		var compositeView = CompositeView
		(bigramTracksOptionsView, Rect(0, (bigramEditor.numTracks-1)*trackHeight, bigramTracksOptionsView_width, trackHeight));

		trackControls.add(compositeView);
		trackControlNames.add(trackName);

		//create widgets

		//name
		name = StaticText(compositeView,Rect(0,0,bigramTracksOptionsView_width,trackHeight/5));
		name.string_(trackName).background_(Color.blue).align_(\center);
		trackControls_name.add(name);

		//buttons
		recButton = Button(compositeView,Rect(0,trackHeight/5, bigramTracksOptionsView_width/4,trackHeight/5));
		recButton.states_([["R",Color.black,Color.grey(0.8)],["R",Color.black,Color.red]]);
		recButton.canFocus_(false);
		trackControls_recButton.add(recButton);

		soloButton = Button(compositeView,Rect(bigramTracksOptionsView_width/2,trackHeight/5, bigramTracksOptionsView_width/4,trackHeight/5));
		soloButton.states_([["S",Color.black,Color.grey(0.8)],["S",Color.black,Color.yellow]]);
		soloButton.canFocus_(false);
		trackControls_soloButton.add(soloButton);
		soloButton.action = { |button|
			var value = button.value;
			if (value == 0 ) {
				// no solo

				var numSolos = 0;

				// get how many buttons are in solo
				bigramEditor.bigramTrackNames.do{ |aTrackName,trackIndex|
					if (aTrackName != trackName) {
						if (trackControls_soloButton.at(trackIndex).value == 1) {
							numSolos = numSolos + 1;
						};
					}
				};

				// if there are no more solos, put all in no mute
				if (numSolos == 0) {
					bigramEditor.bigramTrackNames.do{ |aTrackName,trackIndex|
						trackControls_muteButton.at(trackIndex).valueAction_(0);
					}
				} {
					// if there are more solos, put rest in mute
					bigramEditor.bigramTrackNames.do{ |aTrackName,trackIndex|
						if (trackControls_soloButton.at(trackIndex).value == 0) {
							trackControls_muteButton.at(trackIndex).valueAction_(1);
						};
					}
				};

				// put other tracks in no mute
				/*				bigramEditor.bigramTrackNames.do{ |aTrackName,trackIndex|
				if (aTrackName != trackName) {
				trackControls_muteButton.at(trackIndex).valueAction_(0);
				// if other tracks are in solo, put this in mute
				if (trackControls_soloButton.at(trackIndex).value == 1) {
				muteButton.valueAction_(1);
				};
				}
				}*/

			} {
				// solo

				// put this track in no mute
				muteButton.valueAction_(0);


				// put other tracks in mute, if they are not in solo
				bigramEditor.bigramTrackNames.do{ |aTrackName,trackIndex|
					if (aTrackName != trackName) {
						if (trackControls_soloButton.at(trackIndex).value == 0) {
							trackControls_muteButton.at(trackIndex).valueAction_(1);
						}
					}
				}
			};
		};

		muteButton = Button(compositeView,Rect(3*bigramTracksOptionsView_width/4,trackHeight/5, bigramTracksOptionsView_width/4,trackHeight/5));
		muteButton.states_([["M", Color.black, Color.grey(0.8)],["M", Color.black, Color.green]]);
		muteButton.canFocus_(false);
		trackControls_muteButton.add(muteButton);
		muteButton.action = { |button|
			var value = button.value;
			if (value == 0 ) {
				// no mute
				track.muteAmp = 1;
				this.midiOut.control(track.midiChannel,ctlNum:7,val:track.volume);
			} {
				// mute
				track.muteAmp = 0;
				this.midiOut.control(track.midiChannel,ctlNum:7,val:0);
			};

		};

		//midiOut
		/*		midiOut = PopUpMenu(compositeView,Rect(0,2*trackHeight/5, bigramTracksOptionsView_width,trackHeight/5));
		midiOut.items = midiInstruments;*/
		midiOut = StaticText(compositeView,Rect(0,2*trackHeight/5, bigramTracksOptionsView_width,trackHeight/5));
		midiOut.string_(midiInstruments.at(0)).background_(Color.blue).align_(\center);
		trackControls_midiOut.add(midiOut);
		midiOut.mouseDownAction = {
			var w = Window.new("select MIDI instrument").front;
			var l = ListView.new(w,w.view.bounds);
			l.items = midiInstruments;
			l.value = track.midiProgram;
			l.action = { |list|
				var program = list.value;
				this.setMidiProgram(trackName,program);
				w.close;
			}
		};

		//pan
		pan = Slider(compositeView,Rect(0,3*trackHeight/5, bigramTracksOptionsView_width,trackHeight/5));
		pan.value_(0.5);
		trackControls_pan.add(pan);
		pan.action = { |slider|
			var value = slider.value;
			var midiValue = value.linlin(0,1,0,127).round;
			track.pan_(midiValue);
			this.midiOut.control(track.midiChannel,ctlNum:10,val:midiValue);
		};

		//volume
		volume = Slider(compositeView,Rect(0,4*trackHeight/5, bigramTracksOptionsView_width,trackHeight/5));
		volume.value_(1);
		trackControls_volume.add(volume);
		volume.action = { |slider|
			var value = slider.value;
			var midiValue = value.linlin(0,1,0,127).round;
			track.volume_(midiValue);
			this.midiOut.control(track.midiChannel,ctlNum:7,val:midiValue*track.muteAmp);
		};

	}

	setMidiProgram { |trackName, program|
		var trackIndex = bigramEditor.bigramTrackNames.indexOf(trackName);
		var track = bigramEditor.bigramTracks.at(trackIndex);
		var midiOut = trackControls_midiOut.at(trackIndex);
		midiOut.string_(midiInstruments.at(program));
		track.midiProgram_(program);
		// send message
		this.midiOut.program(track.midiChannel,program);

	}

	removeTrackControls { |trackName|

		// delete trackControls reference
		var index = trackControlNames.indexOf(trackName);

		trackControls.at(index).remove;
		trackControls.removeAt(index);
		trackControlNames.removeAt(index);

		// reorganize elements
		(index..trackControlNames.size-1).do { |i| //indices of "lower" controllers

			var bounds = trackControls[i].bounds;
			trackControls[i].bounds_(Rect(bounds.left,bounds.top-trackHeight, bounds.width,bounds.height));


		};
	}

	removeAllTrackControls {
		trackControls = List.new;
		trackControlNames = List.new;

		// destroy all children views forever
		bigramTracksOptionsView.removeAll;





		/*		bigramTracksOptionsView = CompositeView(bigramView,Rect(0, bigramControlBarsView_height, bigramTracksOptionsView_width,bigramView_height-bigramControlBarsView_height));*/

	}

	///////////////////////// REGION MANAGING /////////////////////////

	openBigramWindow { |track,region,regionName|
		BigramRegionWindow.new(this,track,region,regionName);
	}

	setWindowName {
		window.name_("BigramEditor:  " ++ savePath)
	}

	recalculateCanvasSize {

		//canvas
		bigramCanvas_height = bigramEditor.bigramTracks.size * trackHeight;
		bigramCanvas_width = bigramEditor.numPulses * pulseWidth;
		bigramCanvas.bounds_(Rect(0, bigramControlBarsView_height, bigramCanvas_width, bigramCanvas_height));

		//option tracks
		barTrack.bounds_(Rect(0,0, bigramCanvas_width,bigramControlBarsView_height/3));
		tempoTrack.bounds_(Rect(0,bigramControlBarsView_height/3, bigramCanvas_width,bigramControlBarsView_height/3 ));
		measureTrack.bounds_(Rect(0,2*bigramControlBarsView_height/3, bigramCanvas_width,bigramControlBarsView_height/3 ));

		// track controls
		trackControls.do{ |view,i|
			view.bounds_(Rect(0, i*trackHeight, bigramTracksOptionsView_width, trackHeight));
		};
		trackControls_name.size.postln;

		trackControls_name.do{|o|
			o.postln;
			o.bounds_(Rect(0,0,bigramTracksOptionsView_width,trackHeight/5));
		};
		trackControls_recButton.do{|o|
			o.bounds_(Rect(0,trackHeight/5, bigramTracksOptionsView_width/4,trackHeight/5));
		};
		trackControls_soloButton.do{|o|
			o.bounds_(Rect(bigramTracksOptionsView_width/2,trackHeight/5, bigramTracksOptionsView_width/4,trackHeight/5));
		};
		trackControls_muteButton.do{|o|
			o.bounds_(Rect(3*bigramTracksOptionsView_width/4,trackHeight/5, bigramTracksOptionsView_width/4,trackHeight/5));
		};
		trackControls_midiOut.do{|o|
			o.bounds_(Rect(0,2*trackHeight/5, bigramTracksOptionsView_width,trackHeight/5));
		};
		trackControls_pan.do{|o|
			o.bounds_(Rect(0,3*trackHeight/5, bigramTracksOptionsView_width,trackHeight/5));
		};
		trackControls_volume.do{|o|
			o.bounds_(Rect(0,4*trackHeight/5, bigramTracksOptionsView_width,trackHeight/5));
		};

		this.updateView;
	}


}

// 
